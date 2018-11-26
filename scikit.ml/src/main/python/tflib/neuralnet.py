import tensorflow as tf
import random
import numpy as np
try:
    import exe
    logging = exe.getlogger("NEURAL_NET")
except ImportError:
    import logging
    logging.warn("Couldn't import exe package.")

class NeuralNet:
    """ Implements a tensorflow graph.

    Instance Attributes:
        layers: amount of hidden layer plus 1. e.g. 2 would create a neural net with an input, one hidden and an output layer.
        in_size: input dimension
        out_size: output dimension
        log: a log function. .

        x: tensorflow matrix variable representing the input layer of the net.
        y_: tensorflow matrix variable representing the output layer of the net.
        y: represents labels of training set.

        cross_entropy: Tensorflow Neural Network Cross Entropy Error. Used for backpropagation when training.
        weights_biases_list: list of tensorflow variables to store the variables needed to use for checkpoints.
    """
    log = logging.debug
    layers: int
    in_size: int = -1
    out_size: int = -1
    epochs: int = 20
    learning_rate: float = 0.2
    batch_size:int = 0
    deviation:float = 0.1

    log_device_placement = False
    device = "/cpu:0"

    def __init__(self, layers=2, in_size=-1, out_size=-1):
        self.in_size = int(in_size)
        self.out_size = int(out_size)
        self.layers = int(layers)

    def fit(self, x, y):
        # set in_size and out_size if they are not set:
        if self.in_size <= 0 or self.out_size <= 0:
            self.set_in_out_size(x, y)
        self.nn_create()
        y = self.onehot_y(y)
        self.nn_train(x, y)

    def predict(self, x):
        return self.nn_predict(x)

    def score(self, x, y):
        predictions = self.predict(x)
        score_ = 0
        for i in range(len(predictions)):
            p = predictions[i]
            actual = y[i]
            if p == actual:
                score_ += 1
        self.log("Prediction score %d/%d", score_, len(predictions))
        return score_/len(predictions)

    def set_in_out_size(self, x, y):
        self.out_size = self.max_y(y) + 1
        self.in_size = len(x[0])

    def max_y(self, y):
        current_max = 0
        for y_datapoint in y:
            if current_max < y_datapoint:
                current_max = y_datapoint
        return current_max

    def onehot_y(self, y):
        y_ = list()
        onehotted_classes = list()
        for i in range(self.out_size):
            onehot_class = [0.] * i
            onehot_class.append(1.)
            onehot_class.extend([0.] * (self.out_size - 1 - i))
            onehotted_classes.append(onehot_class)

        for class_index in y:
            y_.append(onehotted_classes[class_index])
        return y_



    def __getstate__(self):
        state = self.get_params()
        checkpoint = self.checkpoint()
        state["checkpoint"] = checkpoint
        return state

    def __setstate__(self, state):
        self.set_params(**state)
        checkpoint = state["checkpoint"]
        if checkpoint is not None:
            self.nn_create(weights_biases_values=checkpoint)

    def get_params(self):
        return {
            "layers" : self.layers,
            "in_size" : self.in_size,
            "out_size" : self.out_size,
            "epochs" : self.epochs,
            "learning_rate" : self.learning_rate,
            "deviation" : self.deviation,
            "batch_size" : self.batch_size,
            "log_device_placement": self.log_device_placement,
            "device": self.device
        }

    def set_params(self, **parameterdict):
        if "layers" in parameterdict:
            self.layers = int(parameterdict["layers"])
        if "epochs" in parameterdict:
            self.epochs = int(parameterdict["epochs"])
        if "learning_rate" in parameterdict:
            self.learning_rate = float(parameterdict["learning_rate"])
        if "batch_size" in parameterdict:
            self.batch_size = int(parameterdict["batch_size"])
        if "deviation" in parameterdict:
            self.deviation = float(parameterdict["deviation"])
        if "in_size" in parameterdict:
            self.in_size = int(parameterdict["in_size"])
        if "out_size" in parameterdict:
            self.out_size = int(parameterdict["out_size"])
        if "log_device_placement" in parameterdict:
            self.log_device_placement = bool(str(parameterdict["log_device_placement"]).lower() == "true" )
        if "device" in parameterdict:
            self.device = str(parameterdict["device"])

    def nn_context(self):
        return self.graph.as_default(), tf.device(self.device)

    def nn_create(self,  weights_biases_values = None):
        """ Creates the skeleton of the neural net.
        """
        self.graph = tf.Graph()
        with self.graph.as_default(), tf.device(self.device):
            # declare the input data placeholders
            self.x = tf.placeholder(tf.float32, [None, self.in_size], name = "input_layer")
            # declare the output data placeholder
            self.y = tf.placeholder(tf.float32, [None, self.out_size], name = "output_layer")
            # empty list
            self.weights_biases_list = []

            load = weights_biases_values is not None

            # declare the weights and biases
            # there will be layers-1 many hidden layers
            # each hidden layer contains the mean of in_size and out_size many nodes
            hidden_out = self.x # points to the output of last layer. In the first iteration prev_layer points to the input layer x.
            prev_nodes_count = self.in_size
            # store how many nodes the layer in the last iteration had. Used for the length of the matrix.
            self.log("Construct a neuronal network:")
            self.log(f"network: {self.layers} layers")
            self.log(f"input layer: {prev_nodes_count} nodes")
            for hidden_index in range(0, self.layers-1):
                hidden_in = hidden_out
                # how many nodes to be used in this layer
                nodes_count = nodes_count_formula(self.layers, self.in_size, self.out_size, hidden_index)
                self.log(f"hidden layer {hidden_index}: {nodes_count} nodes")
                # We initialise the values of the weights using a random normal distribution with a mean of zero and a standard deviation of dev
                if load:
                    # load weights and biases
                    w_i = tf.Variable(weights_biases_values[hidden_index*2])
                    b_i = tf.Variable(weights_biases_values[hidden_index*2+1])
                else:
                    # weight matrix is a 2-dim sqaure matrix: (prev_nodes_count x nodes_count)
                    w_i = tf.Variable(
                        tf.random_normal([prev_nodes_count, nodes_count], stddev = self.deviation),
                        name = f"weight{hidden_index}")
                    # bias vector : (1 x nodes_count)
                    b_i = tf.Variable(
                        tf.random_normal([nodes_count], stddev = self.deviation),
                        name=f"bias{hidden_index}")
                self.weights_biases_list.append(w_i)
                self.weights_biases_list.append(b_i)
                # calculate the output of the hidden layer of this loop iteration
                hidden_out = tf.add(tf.matmul(hidden_in, w_i), b_i)
                hidden_out = tf.tanh(hidden_out, name = f"hidden{hidden_index}_out")
                prev_nodes_count = nodes_count
            self.log(f"output layer: {self.out_size} nodes")
            # now calculate the last hidden layer output - in this case, let's use a softmax activated
            # the weights connecting the last hidden layer to the output layer
            if load:
                # load weights and biases
                w_o = tf.Variable(weights_biases_values[-2])
                b_o = tf.Variable(weights_biases_values[-1])
            else:
                w_o = tf.Variable(tf.random_normal([prev_nodes_count, self.out_size], stddev = self.deviation), name='weightout')
                b_o = tf.Variable(tf.random_normal([self.out_size]), name='biasout')
            self.weights_biases_list.append(w_o)
            self.weights_biases_list.append(b_o)
            # output layer
            self.y_ = tf.nn.softmax(tf.add(tf.matmul(hidden_out, w_o), b_o))
            # clip output node
            y_clipped = tf.clip_by_value(self.y_, 1e-9, 0.9999999)


            # different loss functions:
            cross_entropy = -tf.reduce_mean(
                tf.reduce_sum(self.y * tf.log(y_clipped)
                                + (1 - self.y) * tf.log(1 - y_clipped),
                            axis=1), name = "cross_entropy")
            sum_square = tf.reduce_sum(tf.square(self.y_ - self.y)) # Use minimize cross entropy instead

            mean_square = tf.losses.mean_squared_error(
                self.y,
                self.y_,
                weights=1.0,
                scope=None,
                loss_collection=tf.GraphKeys.LOSSES,
                reduction=tf.losses.Reduction.SUM_BY_NONZERO_WEIGHTS
            )

            self.loss_function = mean_square

    def nn_train(self, x, y):
        """ Trains the neural net with x and y, which are input and output data list.
             This implementation trains the network using the small-batch method. instances are packed into batches of size batch_size.
              The network is trained unsing these batches in a random order. Every datapoint will be used epochs many times.
        """
        with self.graph.as_default(), tf.device(self.device):
            init_op = tf.global_variables_initializer()
            # add an optimiser
            optimiser = tf.train.GradientDescentOptimizer(learning_rate=self.learning_rate).minimize(self.loss_function)

            # define an accuracy assessment operation
            correct_prediction = tf.equal(tf.argmax(self.y, 1), tf.argmax(self.y_, 1))
            accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

            # start the session
            with tf.Session(config=tf.ConfigProto(log_device_placement=self.log_device_placement)) as sess:
                # initialise the variables
                sess.run(init_op)


                total_set_size = min(len(x), len(y))
                # create a shuffled batch order list
                if self.batch_size <= 0 or total_set_size < self.batch_size * 2:
                    batch_order = [0] # no batches.
                else:
                    batch_order = list(range(int(total_set_size/self.batch_size)))
                self.log(f"Starting Training with learning rate = {self.learning_rate} and with {len(batch_order)} many batches for {self.epochs} many epochs.")
                total_avg_cost = 0
                for epoch in range(self.epochs):
                    avg_cost = 0
                    random.shuffle(batch_order) # shuffle the order in each epoch
                    for i in batch_order:
                        if i == batch_order[-1]:
                            # last batch:
                            batch_start = max(i * self.batch_size, 0)
                            batch_end = total_set_size
                        else:
                            batch_start = i * self.batch_size
                            batch_end = min((i+1) * self.batch_size, total_set_size)

                        if batch_end - batch_start < 1:
                            continue # if the batch is empty

                        _, c = sess.run([optimiser, self.loss_function],
                                    feed_dict={ self.x: x[batch_start:batch_end],
                                                self.y: y[batch_start:batch_end]})
                        avg_cost += c / len(batch_order)
                    self.log("Epoch %d average cost: %s", epoch, avg_cost)
                    total_avg_cost += avg_cost/self.epochs


                self.log(f"Total average cost = {total_avg_cost:.3f}")

                accuracy_result =sess.run(accuracy, feed_dict={self.x : x, self.y: y})
                self.log(f"finished training: accuracy:{accuracy_result}")


    def checkpoint(self):
        """
        :return: the state of the neural netwrok model as a list of arrays.
        """
        if not hasattr(self, "weights_biases_list"):
            # model hasn't been trained yet:
            return None
        with self.graph.as_default():
            init_op = tf.global_variables_initializer()
            # start the session
            with tf.Session() as sess:
                # initialise the variables
                sess.run(init_op)
                # the models
                weights_biases_values = []
                for var in self.weights_biases_list:
                    weights_biases_values.append(var.eval())
                return weights_biases_values
            
    def nn_predict(self, x):
        """ Uses the neural network model to predict instances from x.
        """
        with self.graph.as_default(), tf.device(self.device):
            init_op = tf.global_variables_initializer()
            # start the session
            with tf.Session(config=tf.ConfigProto(log_device_placement=self.log_device_placement)) as sess:
                # initialise the variables
                sess.run(init_op)


                total_set_size = len(x)
                #predict every instance
                feed_dict={ self.x: x }
                # output is a list of a list. e.g. output[0] is a list of softmaxed output layer values.
                output = sess.run(self.y_, feed_dict)
                classification = [] # contains classification
                # run through output to decide which class has been classified by the neural net for each instance:
                for instance_output in output:
                    max_index = 0 # index that was predicted
                    for current_index in range(len(instance_output)):
                        instance_node = instance_output[current_index]
                        if instance_node > instance_output[max_index]:
                            max_index = current_index

                    classification.append(max_index) #resolve class name
                return np.asarray(classification)

        
def nodes_count_formula(layers, in_count, out_count, layer_index):
    """This function is called when constructing the neural net graph to calculate the amount of nodes in one layer with the index 'layer_index'.
    Returns the amount of nodes to be used for the given setup.
    Warning: altering this method will lead to constructing networks that won't be compatible with the checkpoints made before, leading to error when loading old data. 
    Note: this formula was developed heuristically to achieve small amount of nodes in each layer to save storage space when saving weights_biases_listmatrices and biases, while also perserving accurate predictions. 
    # Currently the amount of nodes in each layer is calculated using a linear function: (slight change to improve predicitons. See code below)
    #
    # f(x) = m * x + c 
    # x : layer_index
    # f(x) : nodex_count
    # m : gradient is calculated: (y2-y1)/(x2-x1), x1(first layer), y1 = in_count, x2(last layer), y2 = out_count
    # c : contant. is in_count.
    """
    return int((in_count + out_count) / 2)
    # m = ((math.sqrt(out_count) - math.sqrt(in_count)))/layers 
    # c = math.sqrt(in_count)
    # nodes_count = 2 * int((m * (layer_index+1)) + c + 0.5)  
    # return nodes_count 

