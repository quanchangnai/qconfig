package quan.config.definition.parser;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.io.SAXReader;
import quan.config.definition.BeanDefinition;
import quan.config.definition.ClassDefinition;
import quan.config.definition.ConfigDefinition;
import quan.config.definition.ConstantDefinition;
import quan.config.definition.Constants;
import quan.config.definition.EnumDefinition;
import quan.config.definition.FieldDefinition;
import quan.config.definition.IndexDefinition;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于XML的【定义】解析器
 */
public class XmlDefinitionParser extends DefinitionParser {

    {
        definitionEncoding = Charset.defaultCharset().name();
    }

    public XmlDefinitionParser() {
    }

    @Override
    public String getDefinitionType() {
        return "xml";
    }

    @Override
    public int getMinTableBodyStartRow() {
        return 2;
    }

    @Override
    protected void parseFile(File definitionFile) {
        Element rootElement;
        try (InputStreamReader definitionReader = new InputStreamReader(Files.newInputStream(definitionFile.toPath()), definitionEncoding)) {
            rootElement = new SAXReader().read(definitionReader).getRootElement();
            if (rootElement == null || !rootElement.getName().equals("package") && !rootElement.getName().equals("classes")) {
                return;
            }
        } catch (Exception e) {
            String error;
            try {
                error = String.format("解析定义文件[%s]出错", definitionFile.getCanonicalPath());
            } catch (Exception ex) {
                error = String.format("解析定义文件[%s]出错", definitionFile);
            }
            addValidatedError(error);
            logger.error(error, e);
            return;
        }

        String definitionFilePath = getDefinitionParser().definitionFilePaths.get(definitionFile);
        validateElementAttributes(definitionFilePath, rootElement);

        String packageName = null;
        if (rootElement.getName().equals("package")) {
            //以定义文件名作为包名
            packageName = definitionFile.getName().substring(0, definitionFile.getName().lastIndexOf("."));
            if (!Constants.LOWER_PACKAGE_NAME_PATTERN.matcher(packageName).matches()) {
                addValidatedError("定义文件[" + definitionFilePath + "]的文件名格式错误");
            }
        }

        for (int index = 0; index < rootElement.nodeCount(); index++) {
            if (!(rootElement.node(index) instanceof Element)) {
                continue;
            }

            Element classElement = (Element) rootElement.node(index);

            ClassDefinition classDefinition = parseClassDefinition(definitionFilePath, classElement, index);
            if (classDefinition == null) {
                continue;
            }

            parsedClasses.add(classDefinition);

            if (packageName != null) {
                classDefinition.setPackageName(packageName);
            }

            parseClassChildren(classDefinition, classElement);
        }
    }

    private void validateElementAttributes(String definitionFile, Element element, Collection<Object> legalAttributes) {
        List<String> illegalAttributes = new ArrayList<>();

        outer:
        for (int i = 0; i < element.attributeCount(); i++) {
            String attrName = element.attribute(i).getName();
            if (legalAttributes != null) {
                for (Object legalAttribute : legalAttributes) {
                    if (legalAttribute instanceof Pattern && ((Pattern) legalAttribute).matcher(attrName).matches()
                            || legalAttribute instanceof String && attrName.equals(legalAttribute)) {
                        continue outer;
                    }
                }
            }
            illegalAttributes.add(attrName);
        }

        if (!illegalAttributes.isEmpty()) {
            addValidatedError(String.format("定义文件[%s]的元素[%s]不支持属性%s", definitionFile, element.getUniquePath().substring(1), illegalAttributes));
        }
    }

    private void validateElementAttributes(String definitionFile, Element element, Object... legalAttributes) {
        validateElementAttributes(definitionFile, element, Arrays.asList(legalAttributes));
    }

    /**
     * 提取注释
     */
    protected String getComment(Element element, int indexInParent) {
        if (!element.isRootElement() && element.getParent().isRootElement()) {
            List<String> list = new ArrayList<>();
            for (int i = indexInParent - 1; i >= 0; i--) {
                Node node = element.getParent().node(i);
                if (node instanceof Element) {
                    break;
                } else {
                    list.add(node.getText());
                }
            }

            StringBuilder builder = new StringBuilder();

            for (int i = list.size() - 1; i >= 0; i--) {
                builder.append(list.get(i).replaceAll("[\t ]", ""));
            }

            if (StringUtils.isBlank(builder)) {
                return null;
            }

            int start = builder.lastIndexOf("\n\n") + 2;
            if (start < 2) {
                start = 0;
            }

            String comment = builder.substring(start);
            if (StringUtils.isBlank(comment)) {
                return null;
            }

            if (comment.endsWith("\n")) {
                comment = comment.substring(0, comment.length() - 1);
            }
            comment = comment.replace("\n", "，");

            return comment;
        }

        Node commentNode = null;

        if (element.nodeCount() > 0) {
            commentNode = element.node(0);
        } else if (element.getParent().nodeCount() > indexInParent + 1) {
            commentNode = element.getParent().node(indexInParent + 1);
        }

        if (commentNode instanceof Text) {
            String text = commentNode.getText();
            if (!text.startsWith("\n")) {
                return text.trim().split("\n")[0];
            }
        }

        return null;
    }

    private ClassDefinition parseClassDefinition(String definitionFile, Element element, int indexInParent) {
        ClassDefinition classDefinition = createClassDefinition(definitionFile, element);

        if (classDefinition != null) {
            classDefinition.setParser(getDefinitionParser());
            classDefinition.setName(element.attributeValue("name"));
            classDefinition.setLanguageStr(element.attributeValue("lang"));
            classDefinition.setComment(getComment(element, indexInParent));
            classDefinition.setDefinitionFile(definitionFile);
            classDefinition.setVersion(element.asXML().trim());
        }

        return classDefinition;
    }

    protected ClassDefinition createClassDefinition(String definitionFile, Element element) {
        switch (element.getName()) {
            case "enum":
                validateElementAttributes(definitionFile, element, "name");
                return new EnumDefinition();
            case "bean":
                if (element.attribute("parent") != null) {
                    validateElementAttributes(definitionFile, element, "name", "parent");
                } else {
                    validateElementAttributes(definitionFile, element, "name", "delimiter");
                }
                return new BeanDefinition(element.attributeValue("parent"), element.attributeValue("delimiter"));
            case "config":
                validateElementAttributes(definitionFile, element, "name", "table", "lang", "parent");
                return new ConfigDefinition(element.attributeValue("table"), element.attributeValue("parent"));
            default:
                addValidatedError("定义文件[" + definitionFile + "]不支持定义元素:" + element.getName());
                return null;
        }
    }

    protected DefinitionParser getDefinitionParser() {
        return this;
    }

    protected void parseClassChildren(ClassDefinition classDefinition, Element classElement) {
        for (int index = 0; index < classElement.nodeCount(); index++) {
            if (!(classElement.node(index) instanceof Element)) {
                continue;
            }

            Element childElement = (Element) classElement.node(index);
            String childName = childElement.getName();

            if (childName.equals("field") && parseFieldDefinition(classDefinition, childElement, index) != null) {
                continue;
            }

            if (classDefinition instanceof ConfigDefinition) {
                ConfigDefinition configDefinition = (ConfigDefinition) classDefinition;
                switch (childName) {
                    case "index":
                        configDefinition.addIndex(parseIndex(classDefinition, childElement, index));
                        continue;
                    case "constant":
                        parseConstant(configDefinition, childElement, index);
                        continue;
                    case "validations":
                        parseValidations(configDefinition, childElement);
                        continue;
                }
            }

            if (classDefinition instanceof BeanDefinition) {
                if (childName.equals("bean")) {
                    BeanDefinition beanDefinition = (BeanDefinition) parseClassDefinition(classDefinition.getDefinitionFile(), childElement, index);
                    beanDefinition.setParentName(classDefinition.getName());
                    beanDefinition.setPackageName(classDefinition.getPackageName());
                    parsedClasses.add(beanDefinition);
                    parseClassChildren(beanDefinition, childElement);
                    continue;
                }

                if (childName.equals("validations")) {
                    parseValidations((BeanDefinition) classDefinition, childElement);
                    continue;
                }
            }

            addValidatedError("定义文件[" + classDefinition.getDefinitionFile() + "]中的元素[" + classElement.getName() + "]不支持定义子元素:" + childName);
        }
    }

    protected FieldDefinition parseFieldDefinition(ClassDefinition classDefinition, Element fieldElement, int indexInParent) {
        FieldDefinition fieldDefinition = new FieldDefinition();
        fieldDefinition.setParser(classDefinition.getParser());
        classDefinition.addField(fieldDefinition);

        fieldDefinition.setName(fieldElement.attributeValue("name"));
        String types = fieldElement.attributeValue("type");
        fieldDefinition.setTypes(types);
        fieldDefinition.setMin(fieldElement.attributeValue("min"));
        fieldDefinition.setMax(fieldElement.attributeValue("max"));
        fieldDefinition.setEnumValue(fieldElement.attributeValue("value"));
        fieldDefinition.setColumn(fieldElement.attributeValue("column"));
        fieldDefinition.setOptional(fieldElement.attributeValue("optional"));
        fieldDefinition.setIndex(fieldElement.attributeValue("index"));
        fieldDefinition.setDelimiter(fieldElement.attributeValue("delimiter"));
        fieldDefinition.setRef(fieldElement.attributeValue("ref"));
        fieldDefinition.setComment(getComment(fieldElement, indexInParent));
        fieldDefinition.setLanguageStr(fieldElement.attributeValue("lang"));

        String type = types == null ? null : types.split("[:：]")[0];
        List<Object> legalAttributes = new ArrayList<>(Collections.singleton("name"));

        if (classDefinition instanceof BeanDefinition && type != null && Constants.NUMBER_TYPES.contains(type)) {
            legalAttributes.addAll(Arrays.asList("min", "max"));
        }

        if (classDefinition instanceof EnumDefinition) {
            legalAttributes.add("value");
        } else {
            if (classDefinition instanceof BeanDefinition) {
                legalAttributes.addAll(Arrays.asList("type", "ref", "optional", "validation"));

                if (classDefinition instanceof ConfigDefinition) {
                    legalAttributes.addAll(Arrays.asList("lang", "column"));
                    if (type != null && !Constants.COLLECTION_TYPES.contains(type) && !Constants.TIME_TYPES.contains(type)) {
                        //只支持原生类型和枚举类型，但是在这里没法判断是不是枚举类型
                        legalAttributes.add("index");
                    }
                }

                String validation = fieldElement.attributeValue("validation");
                if (!StringUtils.isBlank(validation)) {
                    fieldDefinition.setValidation(validation);
                }
            }

            if (type != null && Constants.COLLECTION_TYPES.contains(type)) {
                legalAttributes.add("delimiter");
            }
        }

        validateElementAttributes(classDefinition.getDefinitionFile(), fieldElement, legalAttributes);

        return fieldDefinition;
    }

    protected IndexDefinition parseIndex(ClassDefinition classDefinition, Element indexElement, int indexInParent) {
        validateElementAttributes(classDefinition.getDefinitionFile(), indexElement, "name", "type", "fields");

        IndexDefinition indexDefinition = new IndexDefinition();
        indexDefinition.setParser(getDefinitionParser());

        indexDefinition.setName(indexElement.attributeValue("name"));
        indexDefinition.setType(indexElement.attributeValue("type"));
        indexDefinition.setFieldNames(indexElement.attributeValue("fields"));

        if (indexInParent >= 0) {
            indexDefinition.setComment(getComment(indexElement, indexInParent));
        }

        return indexDefinition;
    }

    protected void parseConstant(ConfigDefinition configDefinition, Element constantElement, int indexInParent) {
        validateElementAttributes(configDefinition.getDefinitionFile(), constantElement, "name", "enum", "key", "value", "comment");

        ConstantDefinition constantDefinition = new ConstantDefinition();
        constantDefinition.setParser(getDefinitionParser());

        constantDefinition.setName(constantElement.attributeValue("name"));
        constantDefinition.setUseEnum(constantElement.attributeValue("enum"));
        constantDefinition.setKeyField(constantElement.attributeValue("key"));
        constantDefinition.setValueField(constantElement.attributeValue("value"));
        constantDefinition.setCommentField(constantElement.attributeValue("comment"));
        constantDefinition.setVersion(configDefinition.getVersion() + constantElement.asXML().trim());

        if (indexInParent >= 0) {
            constantDefinition.setComment(getComment(constantElement, indexInParent));
        }

        parsedClasses.add(constantDefinition);

        constantDefinition.setOwnerDefinition(configDefinition);
    }

    protected void parseValidations(BeanDefinition beanDefinition, Element childElement) {
        String validations = childElement.getText();
        if (StringUtils.isBlank(validations)) {
            return;
        }

        for (String validation : validations.trim().split("[\r\n;；]")) {
            if (!StringUtils.isBlank(validation)) {
                beanDefinition.getValidations().add(validation.trim());
            }
        }
    }

}
