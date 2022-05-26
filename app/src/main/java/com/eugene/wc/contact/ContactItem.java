package com.eugene.wc.contact;

import com.eugene.wc.protocol.api.contact.ContactId;

import java.util.Objects;

public class ContactItem {

    private ContactId id;
    private String name;
    private boolean status;

    public ContactItem(ContactId id, String name, boolean status) {
        this.name = name;
        this.status = status;
        this.id = id;
    }

    public ContactItem(ContactId id, String name) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public ContactId getId() {
        return id;
    }

    public void setId(ContactId id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactItem that = (ContactItem) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
