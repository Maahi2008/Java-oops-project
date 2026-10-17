
package model;

import java.io.Serializable;
import java.time.LocalDate;

public abstract class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String category;
    private String description;
    private String location;
    private LocalDate date;
    private String reporterName;
    private String contact;

    public Item(String id, String name, String category,
                String description, String location,
                LocalDate date, String reporterName,
                String contact) {

        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.location = location;
        this.date = date;
        this.reporterName = reporterName;
        this.contact = contact;
    }

    public abstract String getType();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getContact() {
        return contact;
    }

    @Override
    public String toString() {
        return getType() + " | " + id + " | " + name
                + " | " + category + " | " + location
                + " | " + date;
    }
}
