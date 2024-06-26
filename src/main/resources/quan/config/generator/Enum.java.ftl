package ${fullPackageName};

/**
<#if comment !="">
 * ${comment}<br/>
</#if>
 * 代码自动生成，请勿手动修改
 */
public enum ${name} {

<#list fields as field>
    <#if field.comment !="">
    /**
     * ${field.comment}
     */
    </#if>
    <#if field_has_next>
    ${field.name}(${field.enumValue}),
    <#else>
    ${field.name}(${field.enumValue});
    </#if>

</#list>

    public final int value;

    ${name}(int value) {
        this.value = value;
    }

    public static ${name} valueOf(int value) {
        switch (value) {
        <#list fields  as field>
            case ${field.enumValue}:
                return ${field.name};
        </#list>
            default:
                return null;
        }
    }

}
