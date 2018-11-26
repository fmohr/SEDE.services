""" Wraps scikit learn libreries. """
from util import wrapping
import sklearn.preprocessing
import exe
logging = exe.getlogger("SL_Service")


# def normalize_labeledinstances(wrappedclass_module, kwargs):
#     """ Wrapper for method.
#     """
#     labeledinstances = kwargs.pop('data_x', None)
#     instances = labeledinstances["instances"]
#     labeledinstances["instances"] = sklearn.preprocessing.normalize(instances, **kwargs)
#
#     dataobject = PASEDataObject("LabeledInstances", labeledinstances)
#     return dataobject


class BaseOptionsSetterMixin(object):
    """ Mixin for setting options.
    The wrapper inheriting this mixin can either wrap a object that has 'get_params' and 'set_params' methods or specify it by itself.
    """
    def set_options(self, stringlist:list):
        """ iterates over the string list and parses each given option and invokes setOption.
        """
        logging.debug("Setting options of %s to: " + str(stringlist), self.name)
        # copy the list so we can safely remove elements from it.
        optionlist = list(stringlist)
        # we create an option dictionary  from the option list.
        optiondict = dict()
        while len(optionlist) >= 2:
            # we take 2 options a, b from the beginning of the list and treat them as the mapping a: b
            field = optionlist.pop(0)
            value = optionlist.pop(0)
            optiondict[field] = value

        if hasattr(self, "set_params"):
            self.set_params(**optiondict)
        else:
            raise RuntimeError("Wrapped instance isn't a optional handler and doesn't define 'set_params'.")

    def set_options_dict(self, optiondict:dict):
        """ invokes set_params .
        """
        logging.debug("Setting options of %s to: " + str(optiondict), self.name)

        if hasattr(self, "set_params"):
            self.set_params(**optiondict)
        else:
            raise RuntimeError("Wrapped instance isn't a optional handler and doesn't define 'set_params'.")


    # def setOption(self, field, value):
    #     """ Uses the get_params and set_params method to assign the given value to the given field.
    #     """
    #     if hasattr(self, "get_params") and hasattr(self, "set_params"):
    #         if field in self.get_params().keys():
    #             # logging.debug("Setting field={} to value={}".format(field,value))
    #             self.set_params(**{field: value})
    #     else:
    #         raise RuntimeError("Wrapped instance isn't a optional handler.")


class BaseClassifierMixin(object):

    def predict_and_score(self, X, normalize=True):
        """ First predicts the input objects using the predict method. Then calculated the accuracy of the model and return it.
        X: Data set
        normalize: If ``False``, return the number of correctly classified samples.
        Otherwise, return the fraction of correctly classified samples.
        """
        y_pred = self.predict(X)
        y_true = X["data_y"]
        score = 0
        # count matches
        for i in range(len(y_pred)):
            if y_pred[i] == y_true[i]:
                score += 1

        if normalize:
            score = float(score) / len(y_pred)  # normalize if needed.
            return round(score, 2)
        else:
            return int(score)


class WrappedClassifier(wrapping.WrappedClassMixin,
                        wrapping.DelegateFunctionsMixin,
                        BaseClassifierMixin,
                        BaseOptionsSetterMixin):
    """ Wraps two functions: train and predict.
    declare_classes has the signature: declare_classes(LabeledInstances)::void
    fit (also callable by 'train') has the new signature:  fit(LabeledInstances)::void
    predict has the new signature: predict(Instances)::LabeledInstances
    Classifiers can deal with classes as strings themselves.
    """
    def __init__(self, classifier_classpath):
        wrapping.WrappedClassMixin.__init__(self, classifier_classpath)
        wrapping.DelegateFunctionsMixin.__init__(self, delegate=self.wrapped_obj)

    def train(self, dataset:dict):
        """ Redirects to fit.
        """
        self.fit(dataset)

    def fit(self, dataset:dict):
        """ dataset is a dict in the format returned by larff.
        """
        logging.trace("Training classifier %s.", self.name)
        # call fit method
        if not isinstance(dataset, dict):
            raise ValueError("Expected dataset to be a dictionary returned by larff.")
        self.wrapped_obj.fit(dataset["data_x"], dataset["data_y"])

    def predict(self, dataset:dict):
        """ dataset is a dict in the format returned by larff.
        """
        if not isinstance(dataset, dict):
            raise ValueError("Expected dataset to be a dictionary returned by larff.")

        prediction = self.wrapped_obj.predict(dataset["data_x"])
        # prediction is an array of classes: ["A", "B", ..]
        return prediction



# TODO Preprocessing and Imputer from scikit

class SkPPWrapper(wrapping.DelegateFunctionsMixin, BaseOptionsSetterMixin):
    """ Wraps the feature selection classes in scikit.
    """
    def __init__(self, wrappedclass_module, kwargs):
        wrappedinstance  = wrappedclass_module() 
        # initialize the DelegateFunctionsMixin with the created wrapped object.
        wrapping.DelegateFunctionsMixin.__init__(self, delegate=wrappedinstance)
        wrapping.BaseOptionsSetterMixin.optionsFromDict(self, kwargs)
    
    def fit(self, X):
        """ X is labeledinstances object.
        """ 
        # call fit method with instances only
        self.delegate.fit(X["instances"], X["labels"]) 

    def transform(self, X):
        output = self.delegate.transform(X["instances"])
        X_copy = dict(X)
        X_copy["instances"] = output
        dataobject = PASEDataObject("LabeledInstances", X_copy)
        return dataobject

    def train(self, X):
        self.fit(X)

    def preprocess(self, X):
        return self.transform(X)

class ImputerWrapper(wrapping.DelegateFunctionsMixin, BaseOptionsSetterMixin):
    """ Wraps the imputer from sk.
    """
    def __init__(self, wrappedclass_module, kwargs):
        wrappedinstance  = wrappedclass_module() 
        # initialize the DelegateFunctionsMixin with the created wrapped object.
        wrapping.DelegateFunctionsMixin.__init__(self, delegate=wrappedinstance)
        wrapping.BaseOptionsSetterMixin.optionsFromDict(self, kwargs)

    def fit(self, X):
        """ X is a labeledinstances object for example: {"instances":[[1.0,2.0],[3.0,4.0]],"labels":["A","B"]}
        """
        # call fit method with instances only
        self.delegate.fit(X["instances"]) 

    def transform(self, X):
        """ X is a labeledinstances object for example: {"instances":[[1.0,2.0],[3.0,4.0]],"labels":["A","B"]}
        """
        imputedinstances = self.delegate.transform(X["instances"])
        X_copy = dict(X)
        X_copy["instances"] = imputedinstances
        dataobject = PASEDataObject("LabeledInstances", X_copy)
        return dataobject


