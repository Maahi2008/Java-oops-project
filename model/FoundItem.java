package model;

import java.time.LocalDate;

public class FoundItem extends Item {
    private static final long serialVersionUID = 3849222380800978291L;

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

    public FoundItem(String id, String name, String category,
                     String description, String location,
                     LocalDate date, String reporterName,
                     String contact, String regNo,
                     String status, String imageUrl) {

        super(id, name, category, description, location,
              date, reporterName, contact, regNo, status, imageUrl);
    }

    @Override
    public String getType() {
        return "FOUND";
    }
}
