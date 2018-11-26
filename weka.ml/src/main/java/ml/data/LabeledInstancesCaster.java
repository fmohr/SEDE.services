package ml.data;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Optional;

/**
 * Provides casting methods for: Instances <-> arff
 */
public class LabeledInstancesCaster {

	public static final String classAttributePrefix ="$class$";

	/**
	 * Reads the arff data from the inputstream and creates a instances object.
	 */
	public Instances cfs_LabeledInstances(InputStream is) throws Exception {
		ConverterUtils.DataSource source = new ConverterUtils.DataSource(is);
		Instances loadedInstaces = (Instances) source.getDataSet();
		removeLabelFromClassAttr(loadedInstaces);
		return loadedInstaces;
	}

	/**
	 * Writes the arff representation of the given dataset into the output stream.
	 */
	public void cts_LabeledInstances(OutputStream os, Instances dataSet) throws IOException {
		addLabelToClassAttr(dataSet);
		OutputStreamWriter writer = new OutputStreamWriter(os);
		writeInstancesAsArff(writer, dataSet);
		removeLabelFromClassAttr(dataSet);
	}

	/**
	 * The same implementation of {@link Instances#toString() Intances.toString()} method
	 * but instead writes to the given writer.<br>
	 * Use this instead of writer.write(dataset.toString()).
	 * @throws IOException thrown by the writer's write method.
	 */
	public void writeInstancesAsArff(Writer writer, Instances dataset) throws IOException{
		writer.write(Instances.ARFF_RELATION);
		writer.write(" ");
		writer.write(Utils.quote(dataset.relationName()));
		writer.write("\n\n");
		for (int i = 0; i < dataset.numAttributes(); i++) {
			writer.write(dataset.attribute(i).toString());
			writer.write("\n");
		}
		writer.append("\n").append(Instances.ARFF_DATA).append("\n");

		int numInstances = dataset.numInstances();
		for (int i = 0; i < numInstances; i++) {
			writer.write(dataset.instance(i).toString());
			if (i < numInstances - 1) {
				writer.write('\n');
			}
		}
		writer.flush();
	}

	private Optional<Attribute> getClassAttribute(Instances dataSet) {
		if(dataSet.classIndex() >= 0) {
			return Optional.of(dataSet.attribute(dataSet.classIndex()));
		} else {
			for(Enumeration<Attribute> e = dataSet.enumerateAttributes();
					e.hasMoreElements();) {
				Attribute attribute = e.nextElement();
				if(attribute.name().startsWith(classAttributePrefix)) {
					return Optional.of(attribute);
				}
			}
		}
		return Optional.empty();
	}

	private void addLabelToClassAttr(Instances dataSet) {
		Optional<Attribute> attr = getClassAttribute(dataSet);
		if(attr.isPresent()) {
			Attribute clsAttr = attr.get();
			String classAttrName = "$class$" + clsAttr.name();
			dataSet.renameAttribute(clsAttr.index(), classAttrName);
		}
	}

	private void removeLabelFromClassAttr(Instances dataSet) {
		Optional<Attribute> classAttr = getClassAttribute(dataSet);
		if(classAttr.isPresent()) {
			Attribute attr = classAttr.get();
			dataSet.setClass(attr);
			if(attr.name().startsWith(classAttributePrefix)) {
				String classAttrName = attr.name().substring(classAttributePrefix.length());
				dataSet.renameAttribute(attr.index(), classAttrName);
			}
		}
	}

}
