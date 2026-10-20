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
    private String regNo;
    private String status;    // "ACTIVE" or "CLAIMED"
    private String imageUrl;  // Optional image URL or base64 data
    private String claimedBy; // Registration number or name of claimant
    private String claimDate; // Date when claim was verified

    public Item(String id, String name, String category,
                String description, String location,
                LocalDate date, String reporterName,
                String contact) {

        this(id, name, category, description, location,
             date, reporterName, contact, "", "ACTIVE", "");
    }

    public Item(String id, String name, String category,
                String description, String location,
                LocalDate date, String reporterName,
                String contact, String regNo) {

        this(id, name, category, description, location,
             date, reporterName, contact, regNo, "ACTIVE", "");
    }

    public Item(String id, String name, String category,
                String description, String location,
                LocalDate date, String reporterName,
                String contact, String regNo,
                String status, String imageUrl) {

        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.location = location;
        this.date = date;
        this.reporterName = reporterName;
        this.contact = contact;
        this.regNo = regNo != null ? regNo : "";
        this.status = (status != null && !status.isEmpty()) ? status : "ACTIVE";
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.claimedBy = "";
        this.claimDate = "";
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

    public String getRegNo() {
        return regNo != null ? regNo : "";
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getClaimedBy() {
        return claimedBy != null ? claimedBy : "";
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public String getClaimDate() {
        return claimDate != null ? claimDate : "";
    }

    public void setClaimDate(String claimDate) {
        this.claimDate = claimDate;
    }

    public void markClaimed(String claimant, String date) {
        this.status = "CLAIMED";
        this.claimedBy = claimant != null ? claimant : "";
        this.claimDate = date != null ? date : LocalDate.now().toString();
    }

    @Override
    public String toString() {
        return getType() + " | " + id + " | " + name
                + " | " + category + " | " + location
                + " | " + date
                + (regNo != null && !regNo.isEmpty() ? " | Reg: " + regNo : "")
                + " | Status: " + getStatus();
    }
}
