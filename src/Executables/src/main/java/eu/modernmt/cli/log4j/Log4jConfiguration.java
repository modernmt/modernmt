package eu.modernmt.cli.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Created by davide on 02/03/16.
 */
public class Log4jConfiguration {

    private static final Level[] VERBOSITY_LEVELS = new Level[]{
            Level.ERROR, Level.INFO, Level.DEBUG, Level.ALL
    };

    public static void setup(int verbosity) {
        if (verbosity < 0 || verbosity >= VERBOSITY_LEVELS.length)
            throw new IllegalArgumentException("Invalid verbosity value: " + verbosity);

        setup(VERBOSITY_LEVELS[verbosity]);
    }

    public static void setup(Level level) {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        AppenderComponentBuilder appenderBuilder = builder.newAppender("StdErr", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_ERR);
        appenderBuilder.add(builder.newLayout("PatternLayout").
                addAttribute("pattern", "%d [%t] %-5level %c{2} - %msg%n%throwable"));
        appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL).
                addAttribute("marker", "FLOW"));

        builder.setStatusLevel(Level.ERROR).add(
                builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
                        .addAttribute("level", level)
        ).add(appenderBuilder);

        builder.add(builder.newLogger("org.apache.logging.log4j", Level.ERROR)
                .add(builder.newAppenderRef("StdErr")).addAttribute("additivity", false));
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("StdErr")));
        Configurator.initialize(builder.build());
    }

}
