package com.simra.konsumgandalf.common.utils.services;

import com.opencsv.bean.CsvBindByName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class CsvUtilServiceTest {

	@TempDir
	Path tempDir;

	@InjectMocks
	private CsvUtilService csvUtilService;

	public static class TestModel {

		@CsvBindByName(column = "column1")
		public String column1;

		@CsvBindByName(column = "column2")
		public String column2;

	}

	@Nested
	class ParseCsvToModelTests {

		@Test
		public void testParseCsvToModel_ValidFile() throws Exception {
			String content = "column1,column2\nvalue1,value2\nvalue3,value4\n";

			List<TestModel> result = csvUtilService.parseCsvToModel(content, TestModel.class);

			assertEquals(2, result.size());
			assertEquals("value1", result.get(0).column1);
			assertEquals("value2", result.get(0).column2);
			assertEquals("value3", result.get(1).column1);
			assertEquals("value4", result.get(1).column2);
		}

	}

}
