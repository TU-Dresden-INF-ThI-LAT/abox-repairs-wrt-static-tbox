package de.tu_dresden.inf.lat.abox_repairs.tools;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.ShortFormProvider;

public class FullIRIShortFormProvider implements ShortFormProvider {
    @Override
    public String getShortForm(OWLEntity owlEntity) {
        return owlEntity.toString();
    }
}
