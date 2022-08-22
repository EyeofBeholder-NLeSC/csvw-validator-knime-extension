package org.nlesc.csvwvalidator;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import com.malyvoj3.csvwvalidator.processor.CsvwProcessor;
import com.malyvoj3.csvwvalidator.config.ProcessorConfig;
import com.malyvoj3.csvwvalidator.processor.ProcessingContext;
import com.malyvoj3.csvwvalidator.processor.ProcessingSettings;
import com.malyvoj3.csvwvalidator.processor.result.ProcessingResult;
import com.malyvoj3.csvwvalidator.processor.result.TextResultWriter;

import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This is an example implementation of the node model of the "CsvwValidator"
 * node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author Ji Qi
 */
public class CsvwValidatorNodeModel extends NodeModel {

	/**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CsvwValidatorNodeModel.class);

	/**
	 * The settings key to retrieve and store settings shared between node dialog
	 * and node model. In this case, the key for the number format String that
	 * should be entered by the user in the dialog.
	 */
	private static final String KEY_METADATA_URL = "metadata_url";

	/**
	 * The default number format String. This default will round to three decimal
	 * places. For an explanation of the format String specification please refer to
	 * https://docs.oracle.com/javase/tutorial/java/data/numberformat.html
	 */
	private static final String DEFAULT_METADATA_URL = "https://w3c.github.io/csvw/tests/test011/tree-ops.csv-metadata.json";

	/**
	 * The settings model to manage the shared settings. This model will hold the
	 * value entered by the user in the dialog and will update once the user changes
	 * the value. Furthermore, it provides methods to easily load and save the value
	 * to and from the shared settings (see: <br>
	 * {@link #loadValidatedSettingsFrom(NodeSettingsRO)},
	 * {@link #saveSettingsTo(NodeSettingsWO)}). <br>
	 * Here, we use a SettingsModelString as the number format is a String. There
	 * are models for all common data types. Also have a look at the comments in the
	 * constructor of the {@link CsvwValidatorNodeDialog} as the settings models are
	 * also used to create simple dialogs.
	 */
	private final SettingsModelString m_metadataUrlSettings = createMetadataUrlSettingsModel();

	/**
	 * Constructor for the node model.
	 */
	protected CsvwValidatorNodeModel() {
		/**
		 * Here we specify how many data input and output tables the node should have.
		 * In this case its one input and one output table.
		 */
		super(1, 1);
	}

	/**
	 * A convenience method to create a new settings model used for the number
	 * format String. This method will also be used in the
	 * {@link CsvwValidatorNodeDialog}. The settings model will sync via the above
	 * defined key.
	 * 
	 * @return a new SettingsModelString with the key for the number format String
	 */
	static SettingsModelString createMetadataUrlSettingsModel() {
		return new SettingsModelString(KEY_METADATA_URL, DEFAULT_METADATA_URL);
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		File file = new File("tmp");
		if (file.exists()) {
			try {
				FileUtils.deleteDirectory(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		file.mkdir();

		String schemaAbsoluteUrl = "https://w3c.github.io/csvw/tests/test011/tree-ops.csv-metadata.json";
		String result = CsvwValidatorNodeModel.validate(null, schemaAbsoluteUrl, true);
		System.out.println(result);
		LOGGER.info(result);
		
		return new BufferedDataTable[] { inData[0] };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] {};
	}

	protected static String validate(String fileUrl, String schemaUrl, boolean isNotStrict) throws Exception {
		// Initialize csvw processor using Spring
		ApplicationContext ctx = new AnnotationConfigApplicationContext(ProcessorConfig.class);
		CsvwProcessor csvwProcessor = ctx.getBean(CsvwProcessor.class);
		TextResultWriter textWriter = ctx.getBean(TextResultWriter.class);

		// Call different processors depending on different input
		ProcessingResult processingResult = null;
		String fileAbsoluteUrl = fileUrl;
		String schemaAbsoluteUrl = schemaUrl;
		ProcessingSettings settings = new ProcessingSettings();
		settings.setStrictMode(!isNotStrict);
		ProcessingContext context = new ProcessingContext(settings);
		if (fileAbsoluteUrl != null && schemaAbsoluteUrl != null) {
			processingResult = csvwProcessor.process(context, fileAbsoluteUrl, schemaAbsoluteUrl);
		} else if (fileAbsoluteUrl != null) {
			processingResult = csvwProcessor.processTabularData(context, fileAbsoluteUrl);
		} else if (schemaAbsoluteUrl != null) {
			processingResult = csvwProcessor.processMetadata(context, schemaAbsoluteUrl);
		}

		// return string result
		byte[] textResult = textWriter.writeResult(processingResult);
		String result = new String(textResult);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		/*
		 * Save user settings to the NodeSettings object. SettingsModels already know
		 * how to save them self to a NodeSettings object by calling the below method.
		 * In general, the NodeSettings object is just a key-value store and has methods
		 * to write all common data types. Hence, you can easily write your settings
		 * manually. See the methods of the NodeSettingsWO.
		 */
		m_metadataUrlSettings.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Load (valid) settings from the NodeSettings object. It can be safely assumed
		 * that the settings are validated by the method below.
		 * 
		 * The SettingsModel will handle the loading. After this call, the current value
		 * (from the view) can be retrieved from the settings model.
		 */
		m_metadataUrlSettings.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Check if the settings could be applied to our model e.g. if the user provided
		 * format String is empty. In this case we do not need to check as this is
		 * already handled in the dialog. Do not actually set any values of any member
		 * variables.
		 */
		m_metadataUrlSettings.validateSettings(settings);
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything that is handed to the output
		 * ports is loaded automatically (data returned by the execute method, models
		 * loaded in loadModelContent, and user settings set through loadSettingsFrom -
		 * is all taken care of). Only load the internals that need to be restored (e.g.
		 * data used by the views).
		 */
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything written to the output ports
		 * is saved automatically (data returned by the execute method, models saved in
		 * the saveModelContent, and user settings saved through saveSettingsTo - is all
		 * taken care of). Save only the internals that need to be preserved (e.g. data
		 * used by the views).
		 */
	}

	@Override
	protected void reset() {
		/*
		 * Code executed on a reset of the node. Models built during execute are cleared
		 * and the data handled in loadInternals/saveInternals will be erased.
		 */
	}
}
