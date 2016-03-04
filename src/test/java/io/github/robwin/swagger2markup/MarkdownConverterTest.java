/*
 * Copyright 2016 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.robwin.swagger2markup;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.robwin.markup.builder.MarkupLanguage;
import io.github.robwin.swagger2markup.config.Swagger2MarkupConfig;
import io.github.robwin.swagger2markup.extension.Swagger2MarkupExtensionRegistry;
import io.github.robwin.swagger2markup.extension.repository.DynamicDefinitionsContentExtension;
import io.github.robwin.swagger2markup.extension.repository.DynamicOperationsContentExtension;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.assertThat;

public class MarkdownConverterTest {

    private static final Logger LOG = LoggerFactory.getLogger(MarkdownConverterTest.class);


    @Test
    public void testSwagger2MarkdownConversion() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(MarkdownConverterTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/docs/markdown/generated");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        Swagger2MarkupConfig config = Swagger2MarkupConfig.ofDefaults()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
        Swagger2MarkupConverter.from(file)
                .withConfig(config)
                .build()
                .intoFolder(outputDirectory);

        //Then
        String[] files = outputDirectory.toFile().list();
        assertThat(files).hasSize(4).containsAll(
                asList("definitions.md", "overview.md", "paths.md", "security.md"));
    }

    @Test
    public void testSwagger2MarkdownConversionWithDescriptions() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(MarkdownConverterTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/docs/markdown/generated");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        Swagger2MarkupConfig config = Swagger2MarkupConfig.ofDefaults()
                .withDefinitionDescriptions(Paths.get("src/docs/markdown/definitions"))
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
        Swagger2MarkupConverter.from(file)
                .withConfig(config)
                .build()
                .intoFolder(outputDirectory);

        //Then
        String[] files = outputDirectory.toFile().list();
        assertThat(files).hasSize(4).containsAll(
                asList("definitions.md", "overview.md", "paths.md", "security.md"));
    }

    @Test
    public void testSwagger2MarkdownConversionWithSeparatedDefinitions() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(MarkdownConverterTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/docs/markdown/generated");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        Swagger2MarkupConfig config = Swagger2MarkupConfig.ofDefaults()
                .withSeparatedDefinitions()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
        Swagger2MarkupConverter.from(file)
                .withConfig(config)
                .build()
                .intoFolder(outputDirectory);

        //Then
        String[] files = outputDirectory.toFile().list();
        assertThat(files).hasSize(5).containsAll(
                asList("definitions", "definitions.md", "overview.md", "paths.md", "security.md"));

        Path definitionsDirectory = outputDirectory.resolve("definitions");
        String[] definitions = definitionsDirectory.toFile().list();
        assertThat(definitions).hasSize(5).containsAll(
                asList("Category.md", "Order.md", "Pet.md", "Tag.md", "User.md"));
    }

    @Test
    public void testSwagger2MarkdownConversionHandlesComposition() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(MarkdownConverterTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/docs/markdown/generated");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        Swagger2MarkupConfig config = Swagger2MarkupConfig.ofDefaults()
                .withSeparatedDefinitions()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
        Swagger2MarkupConverter.from(file)
                .withConfig(config)
                .build()
                .intoFolder(outputDirectory);

        // Then
        String[] files = outputDirectory.toFile().list();
        assertThat(files).hasSize(5).containsAll(
                asList("definitions", "definitions.md", "overview.md", "paths.md", "security.md"));
        Path definitionsDirectory = outputDirectory.resolve("definitions");
        verifyMarkdownContainsFieldsInTables(
                definitionsDirectory.resolve("User.md").toFile(),
                ImmutableMap.<String, Set<String>>builder()
                        .put("User", ImmutableSet.of("id", "username", "firstName",
                                "lastName", "email", "password", "phone", "userStatus"))
                        .build()
        );

    }

    @Test
    public void testSwagger2MarkdownExtensions() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(MarkdownConverterTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/docs/markdown/generated");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        Swagger2MarkupConfig config = Swagger2MarkupConfig.ofDefaults()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
        Swagger2MarkupExtensionRegistry registry = Swagger2MarkupExtensionRegistry.ofEmpty()
                .withExtension(new DynamicDefinitionsContentExtension(Paths.get("src/docs/markdown/extensions")))
                .withExtension(new DynamicOperationsContentExtension(Paths.get("src/docs/markdown/extensions")))
                .build();
        Swagger2MarkupConverter.from(file)
                .withConfig(config)
                .withExtensionRegistry(registry)
                .build()
                .intoFolder(outputDirectory);

        //Then
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("paths.md")))).contains(
                "Pet update request extension");
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("definitions.md")))).contains(
                "Pet extension");

    }

    /**
     * Given a markdown document to search, this checks to see if the specified tables
     * have all of the expected fields listed.
     *
     * @param doc           markdown document file to inspect
     * @param fieldsByTable map of table name (header) to field names expected
     *                      to be found in that table.
     * @throws IOException if the markdown document could not be read
     */
    private static void verifyMarkdownContainsFieldsInTables(File doc, Map<String, Set<String>> fieldsByTable) throws IOException {
        final List<String> lines = Files.readAllLines(doc.toPath(), Charset.defaultCharset());
        final Map<String, Set<String>> fieldsLeftByTable = Maps.newHashMap();
        for (Map.Entry<String, Set<String>> entry : fieldsByTable.entrySet()) {
            fieldsLeftByTable.put(entry.getKey(), Sets.newHashSet(entry.getValue()));
        }
        String inTable = null;
        for (String line : lines) {
            // If we've found every field we care about, quit early
            if (fieldsLeftByTable.isEmpty()) {
                return;
            }

            // Transition to a new table if we encounter a header
            final String currentHeader = getTableHeader(line);
            if (inTable == null || currentHeader != null) {
                inTable = currentHeader;
            }

            // If we're in a table that we care about, inspect this potential table row
            if (inTable != null && fieldsLeftByTable.containsKey(inTable)) {
                // If we're still in a table, read the row and check for the field name
                //  NOTE: If there was at least one pipe, then there's at least 2 fields
                String[] parts = line.split("\\|");
                if (parts.length > 1) {
                    final String fieldName = parts[1];
                    final Set<String> fieldsLeft = fieldsLeftByTable.get(inTable);
                    // Mark the field as found and if this table has no more fields to find,
                    //  remove it from the "fieldsLeftByTable" map to mark the table as done
                    if (fieldsLeft.remove(fieldName) && fieldsLeft.isEmpty()) {
                        fieldsLeftByTable.remove(inTable);
                    }
                }
            }
        }

        // After reading the file, if there were still types, fail
        if (!fieldsLeftByTable.isEmpty()) {
            fail(String.format("Markdown file '%s' did not contain expected fields (by table): %s",
                    doc, fieldsLeftByTable));
        }
    }

    private static String getTableHeader(String line) {
        return line.startsWith("###")
                ? line.replace("###", "").trim()
                : null;
    }
}
