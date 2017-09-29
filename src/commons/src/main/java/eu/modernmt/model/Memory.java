package eu.modernmt.model;

import java.io.Serializable;

/**
 * Created by davide on 06/09/16.
 */
public class Memory implements Serializable {

    private long id;
    private String name;

    public Memory(long id) {
        this(id, null);
    }

    public Memory(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

        Memory memory = (Memory) o;

        return id == memory.id;

    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Memory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
