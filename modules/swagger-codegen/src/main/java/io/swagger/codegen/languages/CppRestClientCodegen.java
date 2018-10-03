package io.swagger.codegen.languages;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.swagger.codegen.languages.helpers.ExtensionHelper.getBooleanValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenParameter;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.utils.ModelUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.apache.commons.lang3.StringUtils;

public class CppRestClientCodegen extends AbstractCppCodegen {

    public static final String DECLSPEC = "declspec";
    public static final String DEFAULT_INCLUDE = "defaultInclude";

    protected String packageVersion = "1.0.0";
    protected String declspec = "";
    protected String defaultInclude = "";

    private final Set<String> parentModels = new HashSet<>();
    private final Multimap<String, CodegenModel> childrenByParent = ArrayListMultimap.create();

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see io.swagger.codegen.CodegenType
     */
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    /**
     * Configures a friendly name for the generator. This will be used by the
     * generator to select the library with the -l flag.
     *
     * @return the friendly name for the generator
     */
    public String getName() {
        return "cpprest";
    }

    /**
     * Returns human-friendly help for the generator. Provide the consumer with
     * help tips, parameters here
     *
     * @return A string value for the help message
     */
    public String getHelp() {
        return "Generates a C++ API client with C++ REST SDK (https://github.com/Microsoft/cpprestsdk).";
    }

    public CppRestClientCodegen() {
        super();

        apiPackage = "io.swagger.client.api";
        modelPackage = "io.swagger.client.model";

        modelTemplateFiles.put("model-header.mustache", ".h");
        modelTemplateFiles.put("model-source.mustache", ".cpp");

        apiTemplateFiles.put("api-header.mustache", ".h");
        apiTemplateFiles.put("api-source.mustache", ".cpp");

        embeddedTemplateDir = templateDir = "cpprest";

        cliOptions.clear();

        // CLI options
        addOption(CodegenConstants.MODEL_PACKAGE, "C++ namespace for models (convention: name.space.model).",
                this.modelPackage);
        addOption(CodegenConstants.API_PACKAGE, "C++ namespace for apis (convention: name.space.api).",
                this.apiPackage);
        addOption(CodegenConstants.PACKAGE_VERSION, "C++ package version.", this.packageVersion);
        addOption(DECLSPEC, "C++ preprocessor to place before the class name for handling dllexport/dllimport.",
                this.declspec);
        addOption(DEFAULT_INCLUDE,
                "The default include statement that should be placed in all headers for including things like the declspec (convention: #include \"Commons.h\" ",
                this.defaultInclude);

        supportingFiles.add(new SupportingFile("modelbase-header.mustache", "", "ModelBase.h"));
        supportingFiles.add(new SupportingFile("modelbase-source.mustache", "", "ModelBase.cpp"));
        supportingFiles.add(new SupportingFile("object-header.mustache", "", "Object.h"));
        supportingFiles.add(new SupportingFile("object-source.mustache", "", "Object.cpp"));
        supportingFiles.add(new SupportingFile("apiclient-header.mustache", "", "ApiClient.h"));
        supportingFiles.add(new SupportingFile("apiclient-source.mustache", "", "ApiClient.cpp"));
        supportingFiles.add(new SupportingFile("apiconfiguration-header.mustache", "", "ApiConfiguration.h"));
        supportingFiles.add(new SupportingFile("apiconfiguration-source.mustache", "", "ApiConfiguration.cpp"));
        supportingFiles.add(new SupportingFile("apiexception-header.mustache", "", "ApiException.h"));
        supportingFiles.add(new SupportingFile("apiexception-source.mustache", "", "ApiException.cpp"));
        supportingFiles.add(new SupportingFile("ihttpbody-header.mustache", "", "IHttpBody.h"));
        supportingFiles.add(new SupportingFile("jsonbody-header.mustache", "", "JsonBody.h"));
        supportingFiles.add(new SupportingFile("jsonbody-source.mustache", "", "JsonBody.cpp"));
        supportingFiles.add(new SupportingFile("httpcontent-header.mustache", "", "HttpContent.h"));
        supportingFiles.add(new SupportingFile("httpcontent-source.mustache", "", "HttpContent.cpp"));
        supportingFiles.add(new SupportingFile("multipart-header.mustache", "", "MultipartFormData.h"));
        supportingFiles.add(new SupportingFile("multipart-source.mustache", "", "MultipartFormData.cpp"));
        supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        supportingFiles.add(new SupportingFile("cmake-lists.mustache", "", "CMakeLists.txt"));

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList("int", "char", "bool", "long", "float", "double", "int32_t", "int64_t"));

        typeMapping = new HashMap<String, String>();
        typeMapping.put("date", "utility::datetime");
        typeMapping.put("DateTime", "utility::datetime");
        typeMapping.put("string", "utility::string_t");
        typeMapping.put("integer", "int32_t");
        typeMapping.put("long", "int64_t");
        typeMapping.put("boolean", "bool");
        typeMapping.put("array", "std::vector");
        typeMapping.put("map", "std::map");
        typeMapping.put("file", "HttpContent");
        typeMapping.put("object", "Object");
        typeMapping.put("binary", "std::string");
        typeMapping.put("number", "double");
        typeMapping.put("UUID", "utility::string_t");

        super.importMapping = new HashMap<String, String>();
        importMapping.put("std::vector", "#include <vector>");
        importMapping.put("std::map", "#include <map>");
        importMapping.put("std::string", "#include <string>");
        importMapping.put("HttpContent", "#include \"HttpContent.h\"");
        importMapping.put("Object", "#include \"Object.h\"");
        importMapping.put("utility::string_t", "#include <cpprest/details/basic_types.h>");
        importMapping.put("utility::datetime", "#include <cpprest/details/basic_types.h>");
    }

    protected void addOption(String key, String description, String defaultValue) {
        CliOption option = new CliOption(key, description);
        if (defaultValue != null)
            option.defaultValue(defaultValue);
        cliOptions.add(option);
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(DECLSPEC)) {
            declspec = additionalProperties.get(DECLSPEC).toString();
        }

        if (additionalProperties.containsKey(DEFAULT_INCLUDE)) {
            defaultInclude = additionalProperties.get(DEFAULT_INCLUDE).toString();
        }

        additionalProperties.put("modelNamespaceDeclarations", modelPackage.split("\\."));
        additionalProperties.put("modelNamespace", modelPackage.replaceAll("\\.", "::"));
        additionalProperties.put("modelHeaderGuardPrefix", modelPackage.replaceAll("\\.", "_").toUpperCase());
        additionalProperties.put("apiNamespaceDeclarations", apiPackage.split("\\."));
        additionalProperties.put("apiNamespace", apiPackage.replaceAll("\\.", "::"));
        additionalProperties.put("apiHeaderGuardPrefix", apiPackage.replaceAll("\\.", "_").toUpperCase());
        additionalProperties.put("declspec", declspec);
        additionalProperties.put("defaultInclude", defaultInclude);
    }

    /**
     * Location to write model files. You can use the modelPackage() as defined
     * when the class is instantiated
     */
    public String modelFileFolder() {
        return outputFolder + "/model";
    }

    /**
     * Location to write api files. You can use the apiPackage() as defined when
     * the class is instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + "/api";
    }

    @Override
    public String toModelImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        } else {
            return "#include \"" + name + ".h\"";
        }
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allSchemas) {
        CodegenModel codegenModel = super.fromModel(name, schema, allSchemas);
        Set<String> oldImports = codegenModel.imports;
        codegenModel.imports = new HashSet<>();
        for (String imp : oldImports) {
            String newImp = toModelImport(imp);
            if (!newImp.isEmpty()) {
                codegenModel.imports.add(newImp);
            }
        }
        return codegenModel;
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation,
            Map<String, Schema> schemas, OpenAPI openAPI) {
        final CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, schemas, openAPI);
        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            ApiResponse methodResponse = findMethodResponse(operation.getResponses());
            if (methodResponse == null) {
                return codegenOperation;
            }
            final Schema responseSchema = getSchemaFromResponse(methodResponse);
            if (responseSchema != null) {
                CodegenProperty cm = fromProperty("response", responseSchema);
                codegenOperation.vendorExtensions.put("x-codegen-response", cm);
                if (cm.datatype == "HttpContent") {
                    codegenOperation.vendorExtensions.put("x-codegen-response-ishttpcontent", true);
                }
            }
        }
        return codegenOperation;
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        if (isFileProperty(property)) {
            property.vendorExtensions.put("x-codegen-file", true);
        }

        if (!isNullOrEmpty(model.parent)) {
            parentModels.add(model.parent);
            if (!childrenByParent.containsEntry(model.parent, model)) {
                childrenByParent.put(model.parent, model);
            }
        }
    }

    protected boolean isFileProperty(CodegenProperty property) {
        return property.baseType.equals("HttpContent");
    }

    @Override
    public String toModelFilename(String name) {
        return initialCaps(name);
    }

    @Override
    public String toApiFilename(String name) {
        return initialCaps(name) + "Api";
    }

    /**
     * Optional - type declaration. This is a String which is used by the
     * templates to instantiate your types. There is typically special handling
     * for different property types
     *
     * @return a string value used as the `dataType` field for model templates,
     *         `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Schema propertySchema) {
        String schemaType = getSchemaType(propertySchema);
        if (propertySchema instanceof ArraySchema) {
            Schema inner = ((ArraySchema) propertySchema).getItems();
            return String.format("%s<%s>", schemaType, getTypeDeclaration(inner));
        } else if (propertySchema instanceof MapSchema) {
            Schema inner = (Schema) propertySchema.getAdditionalProperties();
            return String.format("%s<utility::string_t, %s>", schemaType, getTypeDeclaration(inner));
        }
        if (propertySchema instanceof StringSchema || propertySchema instanceof DateSchema
                || propertySchema instanceof DateTimeSchema || propertySchema instanceof FileSchema
                || languageSpecificPrimitives.contains(schemaType)) {
            return toModelName(schemaType);
        }
        return "std::shared_ptr<" + schemaType + ">";
    }

    @Override
    public String toDefaultValue(Schema propertySchema) {
        if (propertySchema instanceof StringSchema) {
            return "utility::conversions::to_string_t(\"\")";
        } else if (propertySchema instanceof BooleanSchema) {
            return "false";
        } else if (propertySchema instanceof DateSchema) {
            return "utility::datetime()";
        } else if (propertySchema instanceof DateTimeSchema) {
            return "utility::datetime()";
        } else if (propertySchema instanceof NumberSchema) {
            if(SchemaTypeUtil.FLOAT_FORMAT.equals(propertySchema.getFormat())) {
                return "0.0f";
            }
            return "0.0";
        } else if (propertySchema instanceof IntegerSchema) {
            if(SchemaTypeUtil.INTEGER64_FORMAT.equals(propertySchema.getFormat())) {
                return "0L";
            }
            return "0";
        } else if (propertySchema instanceof MapSchema && hasSchemaProperties(propertySchema)) {
            String inner = getSchemaType((Schema) propertySchema.getAdditionalProperties());
            return String.format("std::map<utility::string_t, %s>()", inner);
        } else if (propertySchema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) propertySchema;
            String inner = getSchemaType(arraySchema.getItems());
            if (!languageSpecificPrimitives.contains(inner)) {
                inner = String.format("std::shared_ptr<%s>", inner);
            }
            return String.format("std::vector<%s>()", inner);
        } else if (StringUtils.isNotBlank(propertySchema.get$ref())) {
            return String.format("new %s()", toModelName(propertySchema.get$ref()));
        }
        return "nullptr";
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);

        boolean isPrimitiveType = getBooleanValue(parameter, CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME);
        boolean isListContainer = getBooleanValue(parameter, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME);
        boolean isString = getBooleanValue(parameter, CodegenConstants.IS_STRING_EXT_NAME);

        if (!isPrimitiveType && !isListContainer && !isString && !parameter.dataType.startsWith("std::shared_ptr")) {
            parameter.dataType = "std::shared_ptr<" + parameter.dataType + ">";
        }
    }

    /**
     * Optional - swagger type conversion. This is used to map swagger types in
     * a `Property` into either language specific types via `typeMapping` or
     * into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     * @see io.swagger.v3.oas.models.media.Schema
     */
    @Override
    public String getSchemaType(Schema propertySchema) {
        String swaggerType = super.getSchemaType(propertySchema);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type))
                return toModelName(type);
        } else
            type = swaggerType;
        return toModelName(type);
    }

    @Override
    public String toModelName(String type) {
        if (typeMapping.keySet().contains(type) || typeMapping.values().contains(type)
                || importMapping.values().contains(type) || defaultIncludes.contains(type)
                || languageSpecificPrimitives.contains(type)) {
            return type;
        } else {
            return Character.toUpperCase(type.charAt(0)) + type.substring(1);
        }
    }

    @Override
    public String toApiName(String type) {
        return Character.toUpperCase(type.charAt(0)) + type.substring(1) + "Api";
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    @Override
    public Map<String, Object> postProcessAllModels(final Map<String, Object> models) {

        final Map<String, Object> processed =  super.postProcessAllModels(models);
        postProcessParentModels(models);
        return processed;
    }

    private void postProcessParentModels(final Map<String, Object> models) {
        for (final String parent : parentModels) {
            final CodegenModel parentModel = ModelUtils.getModelByName(parent, models);
            final Collection<CodegenModel> childrenModels = childrenByParent.get(parent);
            for (final CodegenModel child : childrenModels) {
                processParentPropertiesInChildModel(parentModel, child);
            }
        }
    }

    /**
     * Sets the child property's isInherited flag to true if it is an inherited property
     */
    private void processParentPropertiesInChildModel(final CodegenModel parent, final CodegenModel child) {
        final Map<String, CodegenProperty> childPropertiesByName = new HashMap<>(child.vars.size());
        for (final CodegenProperty childProperty : child.vars) {
            childPropertiesByName.put(childProperty.name, childProperty);
        }
        for (final CodegenProperty parentProperty : parent.vars) {
            final CodegenProperty duplicatedByParent = childPropertiesByName.get(parentProperty.name);
            if (duplicatedByParent != null) {
                duplicatedByParent.getVendorExtensions().put(CodegenConstants.IS_INHERITED_EXT_NAME, Boolean.TRUE);
            }
        }
    }

}
