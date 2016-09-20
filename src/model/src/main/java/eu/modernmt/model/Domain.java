package eu.modernmt.model;

import java.io.Serializable;

/**
 * Created by davide on 06/09/16.
 */
public class Domain implements Serializable {

    private int id;
    private String name;

    public Domain(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Domain domain = (Domain) o;

        return id == domain.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

}
