package application.chesstrainerfx.utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Datamodel voor een map-item in de aanvinkbare lijst.
 */
public class MapItem {
    public final String naam;
    public final BooleanProperty aangevinkt = new SimpleBooleanProperty(false);

    public MapItem(String naam) {
        this.naam = naam;
    }

    @Override
    public String toString() {
        return naam;
    }
}
