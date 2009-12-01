/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2009 Pentaho Corporation..  All rights reserved.
 */
package org.pentaho.agilebi.pdi.modeler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.model.LogicalColumn;
import org.pentaho.metadata.model.LogicalModel;
import org.pentaho.metadata.model.LogicalTable;
import org.pentaho.metadata.model.olap.OlapCube;
import org.pentaho.metadata.model.olap.OlapDimension;
import org.pentaho.metadata.model.olap.OlapHierarchy;
import org.pentaho.metadata.model.olap.OlapHierarchyLevel;
import org.pentaho.metadata.model.olap.OlapMeasure;
import org.pentaho.ui.xul.XulEventSourceAdapter;
import org.pentaho.ui.xul.util.AbstractModelList;
import org.pentaho.ui.xul.util.AbstractModelNode;


/**
 * UI model behind the XUL-based interface. This class contains a reference from the context in
 * which the modeling was initiated through an IModelerSource which also provides model generation.
 * 
 * @author nbaker
 *
 */
@SuppressWarnings("unchecked")
public class ModelerWorkspace extends XulEventSourceAdapter{

  private FieldsCollection inPlayFields = new FieldsCollection();
  
  private DimensionMetaDataCollection dimensions = new DimensionMetaDataCollection();
  
  private List<FieldMetaData> availableFields = new ArrayList<FieldMetaData>();
  
  private String sourceName;
  
  private String modelName;
  
  private IModelerSource source;

  private String selectedServer;
  
  private String selectedVisualization;
  
  private String schemaName;
  
  private Domain domain;
  
  private boolean dirty;
  
  // full path to file
  private String fileName;
  
  public ModelerWorkspace(){
    
    inPlayFields.addPropertyChangeListener(new PropertyChangeListener(){
      public void propertyChange(PropertyChangeEvent arg0) {
        fireFieldsChanged();
      }
    });
    dimensions.addPropertyChangeListener(new PropertyChangeListener(){
      public void propertyChange(PropertyChangeEvent arg0) {
        fireDimensionsChanged();
      }
    });
  }
  
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
  
  public String getFileName() {
    return fileName;
  }
  
  //transMeta.getFilename()
  public String getSourceName(){
    return sourceName;
  }
  
  public void setSourceName(String sourceName){
    this.sourceName = sourceName;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    String prevVal = this.modelName;
    this.modelName = modelName;
    setDirty(true);
    this.firePropertyChange("modelName", prevVal, this.modelName);
  }
  
  public boolean isDirty(){
    return dirty;
  }
 
  public void setDirty(boolean dirty){
    boolean prevVal = this.dirty;
    this.dirty = dirty;
    this.firePropertyChange("dirty", prevVal, this.dirty);
  }

  public int getNumberLevels() {
    int v = 0;
    for (DimensionMetaData dim : dimensions) {
      for (HierarchyMetaData hier : dim) {
        for (LevelMetaData lvl : hier.getChildren()) {
          v++;
        }
      }
    }
    return v;
  }
  
  
  public List<FieldMetaData> getAvailableFields() {
    return availableFields;
  }
  
  
  public void setSelectedServer(String server){
    this.selectedServer = server;
  }
  
  public String getSelectedServer(){
    return selectedServer;
  }
  

  public void setSelectedVisualization(String aVisualization) {
  	this.selectedVisualization = aVisualization;
  }  
  
  public String getSelectedVisualization() {
  	return this.selectedVisualization;
  }


  
  public void addDimension(Object obj){

    DimensionMetaData dimension = new DimensionMetaData(obj.toString());
    HierarchyMetaData hierarchy = new HierarchyMetaData(obj.toString());
    hierarchy.setParent(dimension);
    LevelMetaData level = new LevelMetaData(hierarchy, obj.toString());

    // TODO: remove lookup
    LogicalColumn col = findLogicalColumn(obj.toString());
    level.setLogicalColumn(col);
    
    dimension.add(hierarchy);
    hierarchy.add(level);

    addDimension(dimension);
  }
  
  public void addDimension(DimensionMetaData dim){
    this.dimensions.add(dim);
  }
  
  public void addToHeirarchy(Object selectedItem, Object newItem){
    if (selectedItem instanceof LevelMetaData) {
      LevelMetaData sib = (LevelMetaData)selectedItem;
      LevelMetaData level = new LevelMetaData(sib.getParent(), newItem.toString());

      // TODO: remove lookup
      LogicalColumn col = findLogicalColumn(newItem.toString());
      level.setLogicalColumn(col);
      
      sib.getParent().add(level);
      this.firePropertyChange("dimensions", null , dimensions);
    } else if (selectedItem instanceof HierarchyMetaData) {
      HierarchyMetaData hier = (HierarchyMetaData) selectedItem;
      LevelMetaData level = new LevelMetaData(hier, newItem.toString());

      // TODO: remove lookup
      LogicalColumn col = findLogicalColumn(newItem.toString());
      level.setLogicalColumn(col);
      
      hier.add(level);
      this.firePropertyChange("dimensions", null , dimensions);
    } else if (selectedItem instanceof DimensionMetaData) {
      DimensionMetaData dim = (DimensionMetaData)selectedItem;
      HierarchyMetaData hier = null;

      if (dim.size() > 0) {
        hier = dim.get(0);
      } else {
        hier = new HierarchyMetaData(newItem.toString());
        hier.setParent(dim);
        dim.add(hier);
      }
      LevelMetaData level = new LevelMetaData(hier, newItem.toString());
      hier.add(level);
      // TODO: remove lookup
      LogicalColumn col = findLogicalColumn(newItem.toString());
      level.setLogicalColumn(col);
      this.firePropertyChange("dimensions", null , dimensions);
    }
  }

  private void fireFieldsChanged(){
    firePropertyChange("fields", null, inPlayFields);
    setDirty(true);
  }
  
  private void fireDimensionsChanged(){
    firePropertyChange("dimensions", null, dimensions);
    setDirty(true);
  }
  
  // Restore once SetListBox is returning bound objects instead of Strings
  //public void addFieldIntoPlay(Object selectedField){
  //  
  //  FieldMetaData selected = (FieldMetaData) selectedField;
  //  selected.setRowNum(Integer.toString(inPlayFields.size()+1));
  //  
  //  this.inPlayFields.add(selected); //$NON-NLS-1$
  //  
  //  this.firePropertyChange("fields", null, inPlayFields);
  //}
  
  public void addFieldIntoPlay(Object selectedField){
    FieldMetaData meta = new FieldMetaData(Integer.toString(inPlayFields.size()+1), selectedField.toString(), "", selectedField.toString());
    
    // TODO: replace this terrible resolution with better model code.
    LogicalColumn col = findLogicalColumn(selectedField.toString());
    meta.setLogicalColumn(col);
    this.inPlayFields.add(meta); //$NON-NLS-1$
    
    
    this.firePropertyChange("fields", null, inPlayFields);
  }
  
  private LogicalColumn findLogicalColumn(String id){
    LogicalColumn col = null;
    for(LogicalColumn c : domain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns()){
      
      if(c.getName(Locale.getDefault().toString()).equals(id)){
        col = c;
        break;
      }
    }
    return col;
  }
  
  public void removeFieldFromPlay(FieldMetaData field){
    this.inPlayFields.remove(field);
    this.firePropertyChange("fields", null, inPlayFields);
  }
  
  public void setModelSource(IModelerSource source) {
    this.source = source;
  }

  public IModelerSource getModelSource() {
    return source;
  }    

  public List<FieldMetaData> getFields() {
    return inPlayFields;
  }
  
  public void setFields(List<FieldMetaData> fields){
    this.inPlayFields.clear();
    this.inPlayFields.addAll(fields);
  }

 public void refresh() throws ModelerException {
      Domain newDomain = source.generateDomain();
      //ModelerWorkspaceUtil.updateDomain(domain, newDomain);
      
      // Add in new logicalColumns
      outer:
      for(LogicalColumn lc : newDomain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns()){
        boolean exists = false;
        inner:
        for(FieldMetaData fmd : this.availableFields){
          if(fmd.getLogicalColumn().getId().equals(lc.getId())){
            fmd.setLogicalColumn(lc);
            exists = true;
            break inner;
          }
        }
        if(!exists){
          FieldMetaData fm = new FieldMetaData();
          fm.setLogicalColumn(lc);
          fm.setFieldName(lc.getName(Locale.getDefault().toString()));
          availableFields.add(fm);
          Collections.sort(availableFields, new Comparator<FieldMetaData>(){
            public int compare(FieldMetaData arg0, FieldMetaData arg1) {
              return arg0.getLogicalColumn().getId().compareTo(arg1.getLogicalColumn().getId());
            }
          });
        }
      }
      
      // Remove logicalColumns that no longer exist.
      List<FieldMetaData> toRemove = new ArrayList<FieldMetaData>();
      for(FieldMetaData fm : availableFields){
        boolean exists = false;
        LogicalColumn fmlc = fm.getLogicalColumn();
        inner:
        for(LogicalColumn lc : newDomain.getLogicalModels().get(0).getLogicalTables().get(0).getLogicalColumns()){
          if(lc.getId().equals(fmlc.getId())){
            exists = true;
            break inner;
          }
        }
        if(!exists){
          toRemove.add(fm);
        }
      }
      availableFields.removeAll(toRemove);
      fireFieldsChanged();
      
      for(DimensionMetaData dm : dimensions){
        for(HierarchyMetaData hm : dm){
          for(LevelMetaData lm : hm.getChildren()){
            String existingLmId = lm.getLogicalColumn().getId();
            boolean found = false;
            inner:
            for(FieldMetaData fm : availableFields){
              if(fm.getLogicalColumn().getId().equals(existingLmId)){
                found = true;
                break inner;
              }
            }
            if(!found){
              lm.getParent().remove(lm);
            }
          }
        }
      }
      //fireDimensionsChanged();
  }

  public DimensionMetaDataCollection getDimensions(){
    return dimensions;
  }
  
  public String getDatabaseName(){
    return source.getDatabaseName();
  }
  
  public String getSchemaName(){
    return schemaName; 
  }
  
  public void setSchemaName(String schemaName){
    this.schemaName = schemaName;
  }
  
  public void setDomain(Domain d){
    this.domain = d;
    this.dimensions.clear();
    this.inPlayFields.clear();
    this.availableFields.clear();
        
    LogicalTable table = domain.getLogicalModels().get(0).getLogicalTables().get(0);
    for(LogicalColumn c : table.getLogicalColumns()){
      FieldMetaData fm = new FieldMetaData();
      fm.setLogicalColumn(c);
      fm.setFieldName(c.getPhysicalColumn().getName(Locale.getDefault().toString()));
      fm.setDisplayName(c.getName(Locale.getDefault().toString()));
      fm.setAggTypeDesc(c.getAggregationType().toString());
      availableFields.add(fm);
    }
    
    firePropertyChange("availableFields", null, getAvailableFields());
    
    LogicalModel lModel = domain.getLogicalModels().get(0);
    
    if(lModel.getCategories().size() > 0){
      setModelName(lModel.getCategories().get(0).getId());
    }
    
    List<OlapDimension> theDimensions = (List) lModel.getProperty("olap_dimensions");
    if(theDimensions != null) {
	    Iterator<OlapDimension> theDimensionItr = theDimensions.iterator();
	    while(theDimensionItr.hasNext()) {
	    	OlapDimension theDimension = theDimensionItr.next();
	    	
	    	DimensionMetaData theDimensionMD = new DimensionMetaData(theDimension.getName());
	    	
	    	List<OlapHierarchy> theHierarchies = (List) theDimension.getHierarchies();
	    	Iterator<OlapHierarchy> theHierarchiesItr = theHierarchies.iterator();
	    	while(theHierarchiesItr.hasNext()) {
	    		OlapHierarchy theHierarchy =  theHierarchiesItr.next();
	    		HierarchyMetaData theHierarchyMD = new HierarchyMetaData(theHierarchy.getName());
	    		
	    		List<OlapHierarchyLevel> theLevels = theHierarchy.getHierarchyLevels();
	    		Iterator<OlapHierarchyLevel> theLevelsItr = theLevels.iterator();
	    		while(theLevelsItr.hasNext()) {
	    			OlapHierarchyLevel theLevel = theLevelsItr.next();
	    			LevelMetaData theLevelMD = new LevelMetaData(theHierarchyMD, theLevel.getName());

	    			theLevelMD.setParent(theHierarchyMD);
	    			theLevelMD.setLogicalColumn(theLevel.getReferenceColumn());
	    			theHierarchyMD.add(theLevelMD);
	    		}
	    		
	    		theHierarchyMD.setParent(theDimensionMD);
	    		theDimensionMD.add(theHierarchyMD);
	    	}
	    	this.dimensions.add(theDimensionMD);
	    }
    }
    
    List<OlapCube> theCubes = (List) lModel.getProperty("olap_cubes");
    if(theCubes != null) {
	    Iterator<OlapCube> theCubeItr = theCubes.iterator();
	    while(theCubeItr.hasNext()) {
	    	OlapCube theCube = theCubeItr.next();
	    	
	    	List<OlapMeasure> theMeasures = theCube.getOlapMeasures();
	    	Iterator<OlapMeasure> theMeasuresItr = theMeasures.iterator();
	    	while(theMeasuresItr.hasNext()) {
	    		OlapMeasure theMeasure = theMeasuresItr.next();
	    		
	    		FieldMetaData theMeasureMD = new FieldMetaData();
	    		theMeasureMD.setFieldName(theMeasure.getName());

	    		theMeasureMD.setFieldName(theMeasure.getLogicalColumn().getPhysicalColumn().getName(Locale.getDefault().toString()));
	    		theMeasureMD.setDisplayName(theMeasure.getLogicalColumn().getName(Locale.getDefault().toString()));
          theMeasureMD.setAggTypeDesc(theMeasure.getLogicalColumn().getAggregationType().toString());
	        
	    		theMeasureMD.setLogicalColumn(theMeasure.getLogicalColumn());
	    		this.inPlayFields.add(theMeasureMD);
	    	}
	    }
    }
    
  }
  
  public Domain getDomain(){
    return updateDomain();
  }
  
  private Domain updateDomain(){
    // TODO: update domain with changes
    return domain;
  }  

  public static class FieldsCollection extends AbstractModelList<FieldMetaData>{    
  }

  public static class DimensionMetaDataCollection extends AbstractModelNode<DimensionMetaData>{

    private PropertyChangeListener listener = new PropertyChangeListener(){
      public void propertyChange(PropertyChangeEvent evt) {
        fireCollectionChanged();
      }
    };

    protected void fireCollectionChanged() {
      this.changeSupport.firePropertyChange("children", null, this.getChildren());
    }

    @Override
    public void onAdd(DimensionMetaData child) {
      child.addPropertyChangeListener("children", listener);
    }

    @Override
    public void onRemove(DimensionMetaData child) {
      child.removePropertyChangeListener(listener);
    }
    
  }
}