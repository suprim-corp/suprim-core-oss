package sant1ago.dev.suprim.core.query;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects parameters during SQL generation for parameterized queries.
 * Thread-safe for concurrent use within a single query build.
 */
public final class ParameterContext {

    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private int counter = 0;

    /**
     * Add a parameter and return its placeholder name.
     *
     * @param value the parameter value
     * @return the parameter name (e.g., "p1", "p2")
     */
    public String addParameter(Object value) {
        String name = "p" + (++counter);
        parameters.put(name, value);
        return name;
    }

    /**
     * Get all collected parameters.
     *
     * @return map of parameter names to values
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Get current parameter count.
     *
     * @return number of parameters added
     */
    public int getCount() {
        return counter;
    }
}
