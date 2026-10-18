
package model;

import java.time.LocalDate;

public class FoundItem extends Item {

    public FoundItem(String id, String name, String category,
                     String description, String location,
                     LocalDate date, String reporterName,
                     String contact) {

        super(id, name, category, description, location,
              date, reporterName, contact);
    }

    public FoundItem(String id, String name, String category,
                     String description, String location,
                     LocalDate date, String reporterName,
                     String contact, String regNo) {

        super(id, name, category, description, location,
              date, reporterName, contact, regNo);
    }

    @Override
    public String getType() {
        return "FOUND";
    }
}
