package model;

import java.time.LocalDate;

public class LostItem extends Item {
    private static final long serialVersionUID = -8395453581734716096L;

    public LostItem(String id, String name, String category,
                    String description, String location,
                    LocalDate date, String reporterName,
                    String contact) {

        super(id, name, category, description, location,
              date, reporterName, contact);
    }

    public LostItem(String id, String name, String category,
                    String description, String location,
                    LocalDate date, String reporterName,
                    String contact, String regNo) {

        super(id, name, category, description, location,
              date, reporterName, contact, regNo);
    }

    public LostItem(String id, String name, String category,
                    String description, String location,
                    LocalDate date, String reporterName,
                    String contact, String regNo,
                    String status, String imageUrl) {

        super(id, name, category, description, location,
              date, reporterName, contact, regNo, status, imageUrl);
    }

    @Override
    public String getType() {
        return "LOST";
    }
}
