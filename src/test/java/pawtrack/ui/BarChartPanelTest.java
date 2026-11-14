package pawtrack.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class BarChartPanelTest {

    private BarChartPanel chart;

    @BeforeEach
    void setUp() {
        chart = new BarChartPanel();
    }

    @Test
    void testInitialState() {
        assertNotNull(chart);
        assertEquals("", chart.getTitle());
    }

    @Test
    void testSetDataWithEmptyMap() {
        chart.setData("Test", "X", "Y", java.util.Map.of(), "%.0f");
        assertEquals("Test", chart.getTitle());
    }

    @Test
    void testSetDataWithValues() {
        var data = new java.util.LinkedHashMap<String, Number>();
        data.put("A", 10);
        data.put("B", 20);
        data.put("C", 30);
        chart.setData("Test Chart", "Category", "Value", data, "%.0f");
        assertEquals("Test Chart", chart.getTitle());
    }

    @Test
    void testClear() {
        var data = java.util.Map.of("A", 10);
        chart.setData("Test", "X", "Y", data, "%.0f");
        chart.clear("No data");
        assertEquals("No data", chart.getEmptyMessage());
    }

    @Test
    void testValueFormat() {
        var data = java.util.Map.of("Test", 3.14159);
        chart.setData("Test", "X", "Y", data, "%.2f");
        assertEquals("%.2f", chart.getValueFormat());
    }
}