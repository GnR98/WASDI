package wasdi.shared.geoserver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GeoServerStyle {
    @JsonProperty("styles")
    public Styles getStyles() {
        return this.styles; }
    public void setStyles(Styles styles) {
        this.styles = styles; }
    Styles styles;

    public GeoServerStyle() {
    }

    public GeoServerStyle(Styles styles) {
        this.styles = styles;
    }

    public static class Style{
        @JsonProperty("name")
        public String getName() {
            return this.name; }
        public void setName(String name) {
            this.name = name; }
        String name;
        @JsonProperty("href")
        public String getHref() {
            return this.href; }
        public void setHref(String href) {
            this.href = href; }
        String href;

        public Style() {
        }

        public Style(String name, String href) {
            this.name = name;
            this.href = href;
        }
    }

    public static class Styles{
        @JsonProperty("style")
        public List<Style> getStyle() {
            return this.style; }
        public void setStyle(List<Style> style) {
            this.style = style; }
        List<Style> style;

        public Styles() {
        }

        public Styles(List<Style> style) {
            this.style = style;
        }
    }
}