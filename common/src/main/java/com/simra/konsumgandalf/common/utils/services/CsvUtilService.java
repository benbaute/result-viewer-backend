package com.simra.konsumgandalf.common.utils.services;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CsvUtilService {

	private static final Logger _logger = LoggerFactory.getLogger(CsvUtilService.class);

	/**
	 * Parse CSV file to a list of model objects
	 * @param csvContent Path to the CSV file
	 * @param clazz Class of the model object
	 * @return List of model objects
	 */
	public <T> List<T> parseCsvToModel(String csvContent, Class<T> clazz) {
		return parseCsvToModel(csvContent, clazz, false);
	}

	public <T> Optional<T> parseCsvToSingleModel(String csvContent, Class<T> clazz) {
		List<T> models = parseCsvToModel(csvContent, clazz, false);
		return models.isEmpty() ? Optional.empty() : Optional.ofNullable(models.get(0));
	}

	/**
	 * Parse CSV file to a list of model objects
	 * @param csvContent - CSV content
	 * @param clazz - Class of the model object
	 * @param retrying - whether the method is being called after a failed attempt
	 * @param <T>
	 * @return List of model objects
	 */
	private <T> List<T> parseCsvToModel(String csvContent, Class<T> clazz, boolean retrying) {
		try (StringReader reader = new StringReader(csvContent)) {
			CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader).withType(clazz)
				.withIgnoreLeadingWhiteSpace(true)
				.withIgnoreEmptyLine(true)
				.withThrowExceptions(false)
				.build();

			List<T> result = csvToBean.parse();

			if (!retrying && !csvToBean.getCapturedExceptions().isEmpty()) {
				_logger.debug("Captured exceptions during CSV parsing: {}", csvToBean.getCapturedExceptions());
				String cleanedCsv = cleanCsvContent(csvContent);
				return parseCsvToModel(cleanedCsv, clazz, true);
			}
			return result;
		}
		catch (RuntimeException e) {
			throw e;
		}
	}

	/**
	 * Clean CSV content by f.e. removing trailing commas
	 * @param csvContent
	 * @return
	 */
	private String cleanCsvContent(String csvContent) {
		long expectedCommas = getExpectedCommasFromHeader(csvContent);

		return csvContent.lines().map(line -> {
			if (line.endsWith(",")) {
				return line + ",";
			}
			return line;
		}).filter(line -> isValidCsvLine(line, expectedCommas)).collect(Collectors.joining("\n"));
	}

	private long getExpectedCommasFromHeader(String csvContent) {

		String headerLine = csvContent.lines().findFirst().orElse("");

		return headerLine.chars().filter(ch -> ch == ',').count();
	}

	private boolean isValidCsvLine(String line, long expectedCommas) {
		long commaCount = line.chars().filter(ch -> ch == ',').count();

		return commaCount == expectedCommas;
	}

}
