
package model;

import java.time.LocalDate;

public class LostItem extends Item {

    public LostItem(String id, String name, String category,
                    String description, String location,
                    LocalDate date, String reporterName,
                    String contact) {

        super(id, name, category, description, location,
              date, reporterName, contact);
    }

    @Override
    public String getType() {
        return "LOST";
    }
}
