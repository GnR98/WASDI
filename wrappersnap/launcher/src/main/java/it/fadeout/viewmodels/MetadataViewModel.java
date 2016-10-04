package it.fadeout.viewmodels;

import java.util.ArrayList;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class MetadataViewModel {

    public MetadataViewModel(String sName)
    {
        this.name = sName;
    }

    private ArrayList<MetadataViewModel> elements;

    private ArrayList<AttributeViewModel> attributes;

    private String name;

    public ArrayList<MetadataViewModel> getElements() {
        return elements;
    }

    public void setElements(ArrayList<MetadataViewModel> elements) {
        this.elements = elements;
    }

    public ArrayList<AttributeViewModel> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<AttributeViewModel> attributes) {
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
