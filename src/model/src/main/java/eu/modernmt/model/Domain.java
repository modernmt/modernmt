package eu.modernmt.model;

/**
 * Created by davide on 06/09/16.
 */
public class Domain {

    // TODO: this should be replaced to String domain
    private int id;
    private String name;

    public Domain() {
    }

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

}
