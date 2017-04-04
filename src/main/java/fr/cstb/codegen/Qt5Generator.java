package fr.cstb.codegen;

import io.swagger.codegen.*;
import io.swagger.models.properties.*;
import io.swagger.models.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.samskivert.mustache.Mustache.Compiler;

import io.swagger.codegen.examples.ExampleGenerator;
import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.CookieParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.AbstractNumericProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BaseIntegerProperty;
import io.swagger.models.properties.BinaryProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.ByteArrayProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DecimalProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FileProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.PropertyBuilder.PropertyId;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.UUIDProperty;
import io.swagger.util.Json;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Qt5Generator extends DefaultCodegen implements CodegenConfig {
    protected final String PREFIX = "Indigo";
    protected Set<String> foundationClasses = new HashSet<String>();
    // source folder where to write the files
    protected String sourceFolder = "client";
    protected String apiVersion = "1.0.0";
    protected Map<String, String> namespaces = new HashMap<String, String>();
    protected Set<String> systemIncludes = new HashSet<String>();

    public static final String NAMESPACE_NAME = "namespace";
    public static final String NAMESPACE_NAME_DESC = "string, define C++ namespace in which all generated class will be defined, default is Swagger";

    public Qt5Generator() {
        super();

        // set the output folder here
        outputFolder = "generated-code/qt5";

        /*
         * Models.  You can write model files using the modelTemplateFiles map.
         * if you want to create one template for file, you can do so here.
         * for multiple files for model, just put another entry in the `modelTemplateFiles` with
         * a different extension
         */
        modelTemplateFiles.put(
                "model-header.mustache",
                ".h");

        modelTemplateFiles.put(
                "model-body.mustache",
                ".cpp");

        /*
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "api-header.mustache",   // the template to use
                ".h");       // the extension for each file to write

        apiTemplateFiles.put(
                "api-body.mustache",   // the template to use
                ".cpp");       // the extension for each file to write

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        embeddedTemplateDir = templateDir = "Qt5";

        /*
         * Reserved words.  Override this with reserved words specific to your language
         */
        setReservedWordsLowerCase(
                Arrays.asList(
                        "sample1",  // replace with static values
                        "sample2")
        );

        /*
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        additionalProperties.put("apiVersion", apiVersion);
        additionalProperties().put("prefix", PREFIX);

        /*
         * Language Specific Primitives.  These types will not trigger imports by
         * the client generator
         */
        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "bool",
                        "qint32",
                        "qint64",
                        "float",
                        "double")
        );

        supportingFiles.add(new SupportingFile("helpers-header.mustache", sourceFolder, PREFIX + "Helpers.h"));
        supportingFiles.add(new SupportingFile("helpers-body.mustache", sourceFolder, PREFIX + "Helpers.cpp"));
        supportingFiles.add(new SupportingFile("HttpRequest.h.mustache", sourceFolder, PREFIX + "HttpRequest.h"));
        supportingFiles.add(new SupportingFile("HttpRequest.cpp.mustache", sourceFolder, PREFIX + "HttpRequest.cpp"));
        supportingFiles.add(new SupportingFile("modelFactory.mustache", sourceFolder, PREFIX + "ModelFactory.h"));
        supportingFiles.add(new SupportingFile("modelFactory.cpp.mustache", sourceFolder, PREFIX + "ModelFactory.cpp"));
        supportingFiles.add(new SupportingFile("object.mustache", sourceFolder, PREFIX + "Object.h"));
        supportingFiles.add(new SupportingFile("test.cpp.mustache", sourceFolder, "test.cpp"));

        super.typeMapping = new HashMap<String, String>();

        typeMapping.put("date", "QDate");
        typeMapping.put("DateTime", "QDateTime");
        typeMapping.put("string", "QString");
        typeMapping.put("integer", "qint32");
        typeMapping.put("long", "qint64");
        typeMapping.put("boolean", "bool");
        typeMapping.put("array", "QList");
        typeMapping.put("map", "QMap");
        typeMapping.put("file", PREFIX + "HttpRequestInputFileElement");
        typeMapping.put("object", PREFIX + "Object");
        //TODO binary should be mapped to byte array
        // mapped to String as a workaround
        typeMapping.put("binary", "QByteArray");
        typeMapping.put("ByteArray", "QByteArray");

        importMapping = new HashMap<String, String>();

        importMapping.put(PREFIX + "HttpRequestInputFileElement", "#include \"" + PREFIX + "HttpRequest.h\"");

        namespaces = new HashMap<String, String>();

        foundationClasses.add("QString");
        foundationClasses.add("QDate");
        foundationClasses.add("QDateTime");
        foundationClasses.add("QByteArray");

        systemIncludes.add("QString");
        systemIncludes.add("QList");
        systemIncludes.add("QMap");
        systemIncludes.add("QDate");
        systemIncludes.add("QDateTime");
        systemIncludes.add("QByteArray");

        //name formatting options
        cliOptions.add(CliOption.newString(NAMESPACE_NAME, NAMESPACE_NAME_DESC).defaultValue("Swagger"));
    }

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see io.swagger.codegen.CodegenType
     */
    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -l flag.
     *
     * @return the friendly name for the generator
     */
    @Override
    public String getName() {
        return "Qt5";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    @Override
    public String getHelp() {
        return "Generates a Qt5 C++11 client library.";
    }

    @Override
    public String toModelImport(String name) {
        if (namespaces.containsKey(name)) {
            return "using " + namespaces.get(name) + ";";
        } else if (systemIncludes.contains(name)) {
            return "#include <" + name + ">";
        }

        String folder = modelPackage().replace("::", File.separator);
        if (!folder.isEmpty())
            folder += File.separator;

        return "#include \"" + folder + name + ".h\"";
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reseved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;  // add an underscore to the name
    }

    /**
     * Location to write model files.  You can use the modelPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace("::", File.separator);
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace("::", File.separator);
    }

    @Override
    public String toModelFilename(String name) {
        return modelNamePrefix + initialCaps(name);
    }

    @Override
    public String toApiFilename(String name) {
        return modelNamePrefix + initialCaps(name) + "Api";
    }

    /**
     * Optional - type declaration.  This is a String which is used by the templates to instantiate your
     * types.  There is typically special handling for different property types
     *
     * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Property p) {
        String swaggerType = getSwaggerType(p);

        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();
            return getSwaggerType(p) + "<QString, " + getTypeDeclaration(inner) + ">";
        }
        if (foundationClasses.contains(swaggerType)) {
            return swaggerType;
        } else if (languageSpecificPrimitives.contains(swaggerType)) {
            return toModelName(swaggerType);
        } else {
            return swaggerType + "*";
        }
    }

    @Override
    public String toDefaultValue(Property p) {
        if (p instanceof StringProperty) {
            return "new QString(\"\")";
        } else if (p instanceof BooleanProperty) {
            return "false";
        } else if (p instanceof DateProperty) {
            return "NULL";
        } else if (p instanceof DateTimeProperty) {
            return "NULL";
        } else if (p instanceof DoubleProperty) {
            return "0.0";
        } else if (p instanceof FloatProperty) {
            return "0.0f";
        } else if (p instanceof IntegerProperty) {
            return "0";
        } else if (p instanceof LongProperty) {
            return "0L";
        } else if (p instanceof BaseIntegerProperty) {
            // catchall for any other format of the swagger specifiction
            // integer type not explicitly handled above
            return "0";
        } else if (p instanceof DecimalProperty) {
            return "0.0";
        } else if (p instanceof MapProperty) {
            MapProperty ap = (MapProperty) p;
            String inner = getSwaggerType(ap.getAdditionalProperties());
            return "new QMap<QString, " + inner + ">()";
        } else if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            String inner = getSwaggerType(ap.getItems());
            if (!languageSpecificPrimitives.contains(inner)) {
                inner += "*";
            }
            return "new QList<" + inner + ">()";
        }
        // else
        if (p instanceof RefProperty) {
            RefProperty rp = (RefProperty) p;
            return "new " + toModelName(rp.getSimpleRef()) + "()";
        }
        return "NULL";
    }


    /**
     * Optional - swagger type conversion.  This is used to map swagger types in a `Property` into
     * either language specific types via `typeMapping` or into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     * @see io.swagger.models.properties.Property
     */
    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type)) {
                return toModelName(type);
            }
            if (foundationClasses.contains(type)) {
                return type;
            }
        } else {
            type = swaggerType;
        }
        return toModelName(type);
    }

    @Override
    public String toModelName(String type) {
        if (typeMapping.keySet().contains(type) ||
                typeMapping.values().contains(type) ||
                importMapping.values().contains(type) ||
                defaultIncludes.contains(type) ||
                languageSpecificPrimitives.contains(type)) {
            return type;
        } else {
            return modelNamePrefix + Character.toUpperCase(type.charAt(0)) + type.substring(1);
        }
    }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // if it's all uppper case, convert to lower case
        if (name.matches("^[A-Z_]*$")) {
            name = name.toLowerCase();
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        //name = camelize(name,true);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public String toApiName(String type) {
        return modelNamePrefix + Character.toUpperCase(type.charAt(0)) + type.substring(1) + "Api";
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

    /**
     * Overrides postProcessParameter to add a vendor extension "isObject".
     * This is useful when it is a generated Object
     * and add "paramType" for api parameters
     * @param parameter CodegenParameter object to be processed.
     */
    @Override
    public void postProcessParameter(CodegenParameter parameter) {
      // Give the base class a chance to process
      super.postProcessParameter(parameter);

      boolean isObject = parameter.dataType.matches("^"+modelNamePrefix+".*$");

      parameter.vendorExtensions.put("isObject", isObject);

      if (isObject)
      {
        parameter.vendorExtensions.put("paramType", parameter.dataType + "*");
      }
      else
      {
        if (languageSpecificPrimitives.contains(parameter.dataType))
        {
          parameter.vendorExtensions.put("paramType", parameter.dataType);
        }
        else
        {
          parameter.vendorExtensions.put("paramType", "const " + parameter.dataType + "&");
        }
      }

      if (parameter.dataType.matches("^QList.*$"))
      {
        parameter.vendorExtensions.put("isInnerObject", parameter.dataType.matches("^QList<"+modelNamePrefix+".*$"));
        System.out.println("parameter: " + parameter + " isInnerObject: " +parameter.vendorExtensions.get("isInnerObject"));
      }
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property)
    {
      // Give the base class a chance to process
      super.postProcessModelProperty(model, property);

      boolean isObject = property.datatype.matches("^"+modelNamePrefix+".*$");

      property.vendorExtensions.put("isObject", isObject);

      if (isObject)
      {
        property.vendorExtensions.put("paramType", property.datatype);
      }
      else
      {
        if (languageSpecificPrimitives.contains(property.datatype))
        {
          property.vendorExtensions.put("paramType", property.datatype);
        }
        else
        {
          property.vendorExtensions.put("paramType", "const " + property.datatype + "&");
        }
      }

      if (property.datatype.matches("^QList.*$"))
      {
        property.vendorExtensions.put("isInnerObject", property.datatype.matches("^QList<"+modelNamePrefix+".*$"));
        System.out.println("property: " + property + " isInnerObject: " +property.vendorExtensions.get("isInnerObject"));
      }

    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Model> definitions) {
      return fromOperation(path, httpMethod, operation, definitions, null);
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Model> definitions, Swagger swagger) {
      CodegenOperation op = super.fromOperation(path, httpMethod, operation, definitions, swagger);

      if (op.returnType != null)
      {
        System.out.println("operation: " + op + " return type: " + op.returnType );
        if (op.returnType.matches("^QList.*$"))
        {
            op.vendorExtensions.put("isInnerObject", op.returnBaseType.matches(modelNamePrefix+".*$"));

            if (op.returnBaseType.matches("QDateTime"))
              op.vendorExtensions.put("isDateTime", true);
            else if (op.returnBaseType.matches("QDate"))
              op.vendorExtensions.put("isDate", true);
            else if (op.returnBaseType.matches("QString"))
              op.vendorExtensions.put("isString", true);    
            else if (op.returnBaseType.matches("qint32"))
              op.vendorExtensions.put("isInteger", true);    
            else if (op.returnBaseType.matches("qint64"))
              op.vendorExtensions.put("isLong", true);    
            else if (op.returnBaseType.matches("bool"))
              op.vendorExtensions.put("isBool", true);    
            else if (op.returnBaseType.matches("double"))
              op.vendorExtensions.put("isDouble", true);    
            else if (op.returnBaseType.matches("float"))
              op.vendorExtensions.put("isFloat", true);  
            System.out.println("operation: " + op + " isInnerObject: " +op.vendorExtensions.get("isInnerObject") );
        }
        else if (op.returnType.matches("^" + modelNamePrefix + "HttpRequestInputFileElement.*$"))
        {
            System.out.println("Adding isFile to vendorExtensions ");
            op.vendorExtensions.put("isFile", true);
        }
      }

      return op;
    }
  
    @Override
    protected void updatePropertyForArray(CodegenProperty property, CodegenProperty innerProperty) {
        super.updatePropertyForArray(property, innerProperty);
        boolean isObject = property.datatype.matches("^"+modelNamePrefix+".*$");
        property.vendorExtensions.put("isInnerObject", isObject);
        System.out.println("updatePropertyForArray: " + property + " isInnerObject: " +property.vendorExtensions.get("isInnerObject") );

    }

    @Override
    public CodegenResponse fromResponse(String responseCode, Response response) {
      CodegenResponse r = super.fromResponse(responseCode, response);
      if (r.schema != null)
      {
        System.out.println(r);
      }

      return r;
    }
}
