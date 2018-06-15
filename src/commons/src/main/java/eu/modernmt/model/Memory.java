package eu.modernmt.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by davide on 06/09/16.
 */
public class Memory implements Serializable {

    private long id;
    private UUID owner;
    private String name;

    public Memory(long id) {
        this(id, null, null);
    }

    public Memory(long id, String name) {
        this(id, null, name);
    }

    public Memory(long id, UUID owner, String name) {
        this.id = id;
        this.owner = owner;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
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
                ", owner=" + owner +
                ", name='" + name + '\'' +
                '}';
    }

}
