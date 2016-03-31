package net.ids.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.AssignHelper;
import com.google.common.base.Charsets;

/**
 * Helper functions, constants etc for working with <a href="https://github.com/jknack/handlebars.java">Handlebars</a>.
 */
public class HandlebarsUtil {

    private static final Handlebars HANDLEBARS = new Handlebars();

    static {
        HANDLEBARS.registerHelpers(AssignHelper.class);
    }

    /**
     * Compiles the provided template.
     */
    private static Template compile(final String template) {
        try {
            return HANDLEBARS.compileInline(StringUtils.defaultString(template));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Loads the template from the provided classpath resource and compiles it.
     */
    public static Template compile(final InputStream inputStream) {
        try {
            final String template = IOUtils.toString(inputStream, Charsets.UTF_8);
            return compile(template);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Renders the template into the supplied writer.
     */
    public static void render(final Template template, final Map<String, ?> values, final Writer writer) {
        try {
            final Context context = Context.newBuilder(values).resolver(MapValueResolver.INSTANCE, FieldValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE).build();
            template.apply(context, writer);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
