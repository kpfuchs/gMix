/*******************************************************************************
 * gMix open source project - https://svs.informatik.uni-hamburg.de/gmix/
 * Copyright (C) 2014  SVS
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package staticContent.evaluation.simulator.gui.pluginRegistry;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observer;
import java.util.Set;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;

import staticContent.evaluation.simulator.annotations.helper.PossibleValues;
import staticContent.evaluation.simulator.annotations.plugin.Plugin;
import staticContent.evaluation.simulator.annotations.plugin.PluginSuperclass;
import staticContent.evaluation.simulator.annotations.plugin.SimGuiPlugin;
import staticContent.evaluation.simulator.annotations.property.BoolProp;
import staticContent.evaluation.simulator.annotations.property.BoolSimulationProperty;
import staticContent.evaluation.simulator.annotations.property.DoubleProp;
import staticContent.evaluation.simulator.annotations.property.DoubleSimulationProperty;
import staticContent.evaluation.simulator.annotations.property.FloatProp;
import staticContent.evaluation.simulator.annotations.property.FloatSimulationProperty;
import staticContent.evaluation.simulator.annotations.property.IntProp;
import staticContent.evaluation.simulator.annotations.property.IntSimulationProperty;
import staticContent.evaluation.simulator.annotations.property.SimProp;
import staticContent.evaluation.simulator.annotations.property.StringProp;
import staticContent.evaluation.simulator.annotations.property.StringSimulationProperty;

/**
 * The simprop registry
 * 
 * Reads annotations.
 * Build simprop and plugin data models.
 * Manages simprop and plugin changes.
 * 
 * @author alex
 *
 */
public class SimPropRegistry {

	private static Logger logger = Logger.getLogger(SimPropRegistry.class);

	private static SimPropRegistry _instance = null;
	
	private static String currentConfigFile;
	
	private Boolean unsavedChanges = false;

	/**
	 * Singleton
	 * 
	 * @return an instance of {@link SimPropRegistry}
	 */
	public static SimPropRegistry getInstance() {
		if (_instance == null) {
			_instance = new SimPropRegistry();
		}
		return _instance;
	}

	//	private final List<String> pluginLayer;

	@SuppressWarnings("rawtypes")
	private final List<Vector> deferList = new LinkedList<Vector>();

	private int numberOfPluginLayers;

	@SuppressWarnings("unchecked")
	private final Map<String, String>[] pluginLayerMap = new HashMap[100];
	
	/**
	 * Maps all registered properties.
	 * key:	config name of the property
	 * value: property
	 */
	private final Map<String, SimProp> properties = new HashMap<String, SimProp>();
	
	/**
	 * Stores the properties which should be varied.
	 * key:	config name of the property
	 * value: values
	 */
	private final Map<String, String> propertiesToVary = new HashMap<String, String>();
	
	/**
	 * Maps the active plugins. Active plungins are those, which are enabled in the GUI.
	 * key: name of plugin level
	 * value: name of the plugin
	 */
	private final Map<String, String> activePlugins = new HashMap<String, String>();
	
	/**
	 * Maps the active plugins. Active plungins are those, which are enabled in the GUI.
	 * key: name of the plugin
	 * value: name of plugin level
	 */
	private final Map<String, String> activePluginsMapped = new HashMap<String, String>();
	
	/**
	 *  Maps between the display name of a plugin layer and the config name.
	 *  key: display name of the plugin layer
	 *  value: config name of the plugin layer
	 */
	private final Map<String, String> layerMapDisplayNameToConfigName = new LinkedHashMap<String, String>();
	
	/**
	 *  Maps between the display name of a plugin layer and the config name.
	 *  key: config name of the plugin layer
	 *  value: display name of the plugin layer
	 */
	private final Map<String, String> layerMapConfigNameToDisplayName = new LinkedHashMap<String, String>();
	
	/**
	 * Maps the registered plugins to the corresponding plugin layer.
	 * key: config name of the plugin
	 * value: display name of the plugin layer
	 */
	
	private final Map<String, Integer> staticConfigurationDisplay = new HashMap<String, Integer>();

	private final Map<String, String> registeredPlugins = new HashMap<String, String>();

	/**
	 * Maps plugin ids to the corresponding plugin objects
	 * key: id of the plugin
	 * value: plugin object
	 */
	private final Map<String, SimGuiPlugin>  plugins = new HashMap<String, SimGuiPlugin>();
	
	/**
	 * Maps an unique string to possible values (this is useful for predefined values for
	 * the simprops)
	 * key: id of the target
	 * value: possibles Values (comma separated Strings)
	 */
	private final Map<String, String> possibleValueTargets = new HashMap<String, String>();
	
	/**
	 * Maps a layer display name to the order of the layer
	 * key: layer display name (this is like a group name for a bunch of plugins)
	 * value: order (position in the gui)
	 */
	private final Map<String, Integer>  layerMapDisplayNameToOrder = new HashMap<String, Integer>();
	
	/**
	 * Maps a layer config name to the order of the layer
	 * key: layer config name (layers can be considered as an abstraction for a group of plugins)
	 * value: order (position in the gui)
	 */
	private final Map<String, Integer>  layerMapConfigNameToOrder = new HashMap<String, Integer>();

	/**
	 * Maps a layer name to the 
	 * key: layer name
	 * value: a bool that indicates whether the layer is static or dynamic (this influences
	 * the section in which corresponding gui elements are displayed)
	 */
	private final Map<String, Boolean> isStaticLayerMap = new HashMap<String, Boolean>();

	// TODO: Merge both order functions
	/**
	 * @return the mapping between the order (position in the gui) 
	 * and the display name of a layer
	 */
	public Map<String, Integer> getLayerMapDisplayNameToOrder() {
		return this.layerMapDisplayNameToOrder;
	}

	/**
	 * @return the mapping between the order (position in the gui) 
	 * and the config name of a layer
	 */
	public Map<String, Integer> getLayerMapConfigNameToOrder() {
		return this.layerMapConfigNameToOrder;
	}
	/**
	 * @return a map that indicates if a layer is static or not.
	 * static layers are those which are not directly connected 
	 * to a plugin.
	 */
	public Map<String, Boolean> getIsStaticLayerMap() {
		return this.isStaticLayerMap;
	}

	/**
	 * constructor of the simpropregistry class.
	 */
	private SimPropRegistry() {
		
		// there are some properties that cant be caught by annotations
		this.propertiesToVary.put("PROPERTY_TO_VARY", "");
		this.propertiesToVary.put("VALUES_FOR_THE_PROPERTY_TO_VARY", "");
		this.propertiesToVary.put("USE_SECOND_PROPERTY_TO_VARY", "");
		this.propertiesToVary.put("SECOND_PROPERTY_TO_VARY", "");
		this.propertiesToVary.put("VALUES_FOR_THE_SECOND_PROPERTY_TO_VARY", "");
		
		// scan predefined enum values
		this.scanForHelpers();
		
		this.numberOfPluginLayers = 0;

		// scan plugins (dynamic)
		this.scanForPluginProperties();

		// process defered simprops
		this.processDefered();
		
		// scan static properties
		// this has to be done at least because it might
		// be possible that some properties go into
		// dynamic plugin sections
		this.scanForStaticProperties();
		
//		this.toString(); // just for debugging
	}
	
	/**
	 * Scans the enum values of annotated enums
	 * e.g. desired experiments
	 */
	private void scanForHelpers() {
		
		Reflections reflectionsPlugins = new Reflections(
				ClasspathHelper.forPackage("staticContent.evaluation.simulator"),
				new TypeAnnotationsScanner());
		
		Reflections reflectionsPlugins2 = new Reflections(
				ClasspathHelper.forPackage("userGeneratedContent.simulatorPlugIns"),
				new TypeAnnotationsScanner());
		
		reflectionsPlugins = reflectionsPlugins.merge(reflectionsPlugins2);

		// Look for classes with PluginAnnotation
		Set<Class<?>> types = reflectionsPlugins.getTypesAnnotatedWith(staticContent.evaluation.simulator.annotations.helper.PossibleValues.class);

		for (Class<?> target : types) {
			String tmp = "";
			
			// for all fields check if it is an enum
			// if so concatenate the enum constant to a comaseperated list of enum values
			for (Field f : target.getFields()){
				if (f.isEnumConstant())
					tmp = tmp + f.getName() + ",";
			}
			possibleValueTargets.put(target.getAnnotation(PossibleValues.class).id(),tmp);
			logger.log( Level.DEBUG ,"Registered PossibleValuesTarget: " + target.getAnnotation(PossibleValues.class).id());
		}
	}

	// TODO: remove this peace of code
	/**
	 * @return the entry set of all simprops
	 */
	public Set<Entry<String, SimProp>> getAllSimProps() {
		return this.getProperties().entrySet();
	}
	
	public Map<String, String>[] getPluginLayerMap() {
		return this.pluginLayerMap;
	}

	// TODO: remove this peace of code
	/**
	 * searches a simprop with a specific id
	 * @param key is the id of a simprop 
	 * @return the correcponding simprop
	 */
	public SimProp getValue(String key) {

		return this.getProperties().get(key);
	}

	
	/**
	 * registers a plugin
	 * @param s simprop
	 * @param isSuperClass has to be true if s is a property that is defined in a class annotated as a pluginsuperclass
	 * @param isGlobal has to be true if s has a global (not static) annotation
	 * @param pluginLayer id of the corresponding layer (plugin group) 
	 */
	private void register(SimProp s, boolean isSuperClass, boolean isGlobal, String pluginLayer) {
		if (this.getProperties().containsKey(s.getPropertyID()) &&
				!this.getProperties().get( s.getPropertyID() ).getPluginLayerID().equals(this.displayNameToConfigName(pluginLayer))) {

			// Error case
			
			GraphicsDevice graphicsDevice = GraphicsEnvironment
					.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			int x = graphicsDevice.getDisplayMode().getWidth();
			int y = graphicsDevice.getDisplayMode().getHeight();

			if ( !this.getProperties().get( s.getPropertyID() ).equals(pluginLayer) ) {
				JOptionPane alert = new JOptionPane("Redefinition of property '" + s.getPropertyID()
						+ "' at superclass level detected \n (" +
						this.getProperties().get( s.getPropertyID() ).getPluginLayerID() + "" +
						", " + this.displayNameToConfigName(pluginLayer) + ") \n" +
						"Please fix the problem!");

				JDialog dialog = alert.createDialog(null, "");
				int w = dialog.getWidth();
				int h = dialog.getHeight();
				dialog.setLocation((x / 2) - (w / 2), (y / 2) - (h / 2));
				dialog.setVisible(true);
				System.exit(-1);
			}else{
				JOptionPane alert = new JOptionPane("Redefinition of " + s.getPropertyID()
						+ " at pluginlevel (" + s.getPluginID() + ") detected!");

				JDialog dialog = alert.createDialog(null, "");
				int w = dialog.getWidth();
				int h = dialog.getHeight();
				dialog.setLocation((x / 2) - (w / 2), (y / 2) - (h / 2));
				dialog.setVisible(true);
			}
			
		
		} else if ( !this.getProperties().containsKey(s.getPropertyID()) && isSuperClass) {
			// superclass case
			logger.log(Level.DEBUG,  "Associate superclass property " + s.getPropertyID() + " with " + s.getPluginLayerID());
			s.setIsGlobal(true);
			this.getProperties().put(s.getPropertyID(), s);
		} else if ( !this.getProperties().containsKey(s.getPropertyID()) && isGlobal ) {
			// global case
			logger.log(Level.DEBUG,  s.getPropertyID() + " with " + s.getPluginLayerID() + " is forced to be global");
			s.setIsGlobal(true);
			this.getProperties().put(s.getPropertyID(), s);
		} else {
			// normal case
			logger.log(Level.DEBUG, "Register property (" + s.getPropertyID() + ", " + s.getPluginID() + ", " + s.getPluginLayerID() + ")");
			s.setIsGlobal(false);
			this.getProperties().put(s.getPropertyID(), s);
		}
	}

	/**
	 * Scans static simulation properties. Only handles PropertyAnnoations with inject parameter.
	 */
	private void scanForStaticProperties() {

		Reflections reflections = new Reflections(
				ClasspathHelper.forPackage("userGeneratedContent.simulatorPlugIns"),
				new FieldAnnotationsScanner());		
		
		Set<Field> fields = reflections.getFieldsAnnotatedWith(staticContent.evaluation.simulator.annotations.property.IntSimulationProperty.class);
		fields.addAll(reflections.getFieldsAnnotatedWith(staticContent.evaluation.simulator.annotations.property.FloatSimulationProperty.class));
		fields.addAll(reflections.getFieldsAnnotatedWith(staticContent.evaluation.simulator.annotations.property.DoubleSimulationProperty.class));
		fields.addAll(reflections.getFieldsAnnotatedWith(staticContent.evaluation.simulator.annotations.property.StringSimulationProperty.class));
		fields.addAll(reflections.getFieldsAnnotatedWith(staticContent.evaluation.simulator.annotations.property.BoolSimulationProperty.class));

		List<Class<? extends Annotation> > annotationTypes = new LinkedList<Class<? extends Annotation> >();
		annotationTypes.add( IntSimulationProperty.class );
		annotationTypes.add( BoolSimulationProperty.class );
		annotationTypes.add( FloatSimulationProperty.class );
		annotationTypes.add( DoubleSimulationProperty.class );
		annotationTypes.add( StringSimulationProperty.class );

		SimProp property;
		boolean globalProperty = true;
		for ( Field field : fields ){
			Annotation[] a = field.getAnnotations();
			for (Annotation element : a) {

				if (element.annotationType() == BoolSimulationProperty.class) {

					BoolSimulationProperty annotation = field.getAnnotation(BoolSimulationProperty.class);
					if ( !annotation.inject().equals("") ){

						InjectionParser injection = new InjectionParser( annotation.inject(), annotation.key() );

						String layerDisplayName = injection.getLayerDisplayName();
						String layerConfigName = injection.getLayerConfigName();
						String pluginDisplayName = injection.getPluginDisplayName();
						String pluginConfigName = injection.getPluginConfigName();
						int layerPosition = injection.getLayerPosition();
//						int pluginPosition = injection.getPluginPosition();

						property = new BoolProp();
						property.setId(annotation.key());
						property.setName(annotation.name());
						property.setTooltip("Key: " + annotation.key() + "\n " + annotation.tooltip());
						property.setPluginLayerID(layerConfigName);
						property.setPluginID(pluginConfigName);
						property.setEnable_requirements(annotation.enable_requirements());
						property.setValue_requirements(annotation.value_requirements());
						property.setEnable(true);
						property.setInfo(annotation.info());
						property.setPosition(annotation.position());

						globalProperty = annotation.global() || injection.isGlobalProperty();
						property.setIsGlobal(globalProperty);
						property.setIsStatic(annotation.isStatic());
						property.isPropertyToVary(annotation.property_to_vary());

						if(property.isStatic()){
							property.setPluginID("");
						}

						this.getProperties().put(property.getPropertyID(), property);

						if ( !this.getLayerMapDisplayNameToConfigName().containsKey(layerDisplayName)){
							logger.log( Level.DEBUG , "Register plugin layer (" + layerConfigName + ", " + layerDisplayName + ")");
							logger.log( Level.DEBUG, "Set position for injected plugin layer " + layerConfigName + " to " + layerPosition);
							this.getLayerMapDisplayNameToConfigName().put(layerDisplayName, layerConfigName);
							this.layerMapConfigNameToDisplayName.put(layerConfigName, layerDisplayName);
							this.layerMapDisplayNameToOrder.put( layerDisplayName, layerPosition );
							this.layerMapConfigNameToOrder.put( layerConfigName, layerPosition );
							this.isStaticLayerMap.put(layerDisplayName,property.isStatic());
						} else if ( !globalProperty  && !property.isStatic()){
							this.registerPlugin(pluginDisplayName, layerDisplayName, true);
						}
					}
				}

				if (element.annotationType() == IntSimulationProperty.class) {

					IntSimulationProperty annotation = field.getAnnotation(IntSimulationProperty.class);
					if ( !annotation.inject().equals("") ){

						InjectionParser injection = new InjectionParser( annotation.inject(), annotation.key() );

						String layerDisplayName = injection.getLayerDisplayName();
						String layerConfigName = injection.getLayerConfigName();
						String pluginDisplayName = injection.getPluginDisplayName();
						String pluginConfigName = injection.getPluginConfigName();
						int layerPosition = injection.getLayerPosition();
//						int pluginPosition = injection.getPluginPosition();

						property = new IntProp();
						property.setId(annotation.key());
						property.setName(annotation.name());
						property.setTooltip("Key: " + annotation.key() + "\n " + annotation.tooltip());
						property.setPluginLayerID(layerConfigName);
						property.setPluginID(pluginConfigName);
						property.setEnable_requirements(annotation.enable_requirements());
						property.setValue_requirements(annotation.value_requirements());
						property.setEnable(true);
						property.setInfo(annotation.info());
						property.setPosition(annotation.position());

						globalProperty = annotation.global() || injection.isGlobalProperty();
						property.setIsGlobal(globalProperty);
						property.setIsStatic(annotation.isStatic());
						property.isPropertyToVary(annotation.property_to_vary());

						if(property.isStatic()){
							property.setPluginID("");
						}

						// IntProp specific annotation processing
						((IntProp) property).setMinValue(annotation.min());
						((IntProp) property).setMaxValue(annotation.max());
						((IntProp) property).setEnableAuto(annotation.enableAuto());
						((IntProp) property).setEnableUnlimited(annotation.enableUnlimited());
						((IntProp) property).setStepSize(annotation.stepSize());
						((IntProp) property).setGuiElement(annotation.guiElement());

						this.getProperties().put(property.getPropertyID(), property);

						if ( !this.getLayerMapDisplayNameToConfigName().containsKey(layerDisplayName)){
							logger.log( Level.DEBUG , "Register plugin layer (" + layerConfigName + ", " + layerDisplayName + ")");
							logger.log( Level.DEBUG, "Set position for injected plugin layer " + layerConfigName + " to " + layerPosition);
							this.getLayerMapDisplayNameToConfigName().put(layerDisplayName, layerConfigName);
							this.layerMapConfigNameToDisplayName.put(layerConfigName, layerDisplayName);
							this.layerMapDisplayNameToOrder.put( layerDisplayName, layerPosition );
							this.layerMapConfigNameToOrder.put( layerConfigName, layerPosition );
							this.isStaticLayerMap.put(layerDisplayName,property.isStatic());

						} else if ( !globalProperty  && !property.isStatic()){
							this.registerPlugin(pluginDisplayName, layerDisplayName, true);
						}
					}
				}

				if (element.annotationType() == FloatSimulationProperty.class) {

					FloatSimulationProperty annotation = field.getAnnotation(FloatSimulationProperty.class);
					if ( !annotation.inject().equals("") ){

						InjectionParser injection = new InjectionParser( annotation.inject(), annotation.key() );

						String layerDisplayName = injection.getLayerDisplayName();
						String layerConfigName = injection.getLayerConfigName();
						String pluginDisplayName = injection.getPluginDisplayName();
						String pluginConfigName = injection.getPluginConfigName();
						int layerPosition = injection.getLayerPosition();
//						int pluginPosition = injection.getPluginPosition();

						property = new FloatProp();
						property.setId(annotation.key());
						property.setName(annotation.name());
						property.setTooltip("Key: " + annotation.key() + "\n " + annotation.tooltip());
						property.setPluginLayerID(layerConfigName);
						property.setPluginID(pluginConfigName);
						property.setEnable_requirements(annotation.enable_requirements());
						property.setValue_requirements(annotation.value_requirements());
						property.setEnable(true);
						property.setInfo(annotation.info());
						property.setPosition(annotation.position());

						globalProperty = annotation.global() || injection.isGlobalProperty();
						property.setIsGlobal(globalProperty);
						property.setIsStatic(annotation.isStatic());
						property.isPropertyToVary(annotation.property_to_vary());

						if(property.isStatic()){
							property.setPluginID("");
						}

						((FloatProp) property).setMinValue(annotation.min());
						((FloatProp) property).setMaxValue(annotation.max());
						((FloatProp) property).setEnableAuto(annotation.enableAuto());
						((FloatProp) property).setEnableUnlimited(annotation.enableUnlimited());
						((FloatProp) property).setStepSize(annotation.stepSize());
						((FloatProp) property).setGuiElement(annotation.guiElement());
						
						this.getProperties().put(property.getPropertyID(), property);

						if ( !this.getLayerMapDisplayNameToConfigName().containsKey(layerDisplayName)){
							logger.log( Level.DEBUG , "Register plugin layer (" + layerConfigName + ", " + layerDisplayName + ")");
							logger.log( Level.DEBUG, "Set position for injected plugin layer " + layerConfigName + " to " + layerPosition);
							this.getLayerMapDisplayNameToConfigName().put(layerDisplayName, layerConfigName);
							this.layerMapConfigNameToDisplayName.put(layerConfigName, layerDisplayName);
							this.layerMapDisplayNameToOrder.put( layerDisplayName, layerPosition );
							this.layerMapConfigNameToOrder.put( layerConfigName, layerPosition );
							this.isStaticLayerMap.put(layerDisplayName,property.isStatic());

						} else if ( !globalProperty  && !property.isStatic()){
							this.registerPlugin(pluginDisplayName, layerDisplayName, true);
						}
					}
				}

				if (element.annotationType() == DoubleSimulationProperty.class) {

					DoubleSimulationProperty annotation = field.getAnnotation(DoubleSimulationProperty.class);
					if ( !annotation.inject().equals("") ){

						InjectionParser injection = new InjectionParser( annotation.inject(), annotation.key() );

						String layerDisplayName = injection.getLayerDisplayName();
						String layerConfigName = injection.getLayerConfigName();
						String pluginDisplayName = injection.getPluginDisplayName();
						String pluginConfigName = injection.getPluginConfigName();
						int layerPosition = injection.getLayerPosition();
//						int pluginPosition = injection.getPluginPosition();

						property = new DoubleProp();
						property.setId(annotation.key());
						property.setName(annotation.name());
						property.setTooltip("Key: " + annotation.key() + "\n " + annotation.tooltip());
						property.setPluginLayerID(layerConfigName);
						property.setPluginID(pluginConfigName);
						property.setEnable_requirements(annotation.enable_requirements());
						property.setValue_requirements(annotation.value_requirements());
						property.setEnable(true);
						property.setInfo(annotation.info());
						property.setPosition(annotation.position());

						globalProperty = annotation.global() || injection.isGlobalProperty();
						property.setIsGlobal(globalProperty);
						property.setIsStatic(annotation.isStatic());
						property.isPropertyToVary(annotation.property_to_vary());

						if(property.isStatic()){
							property.setPluginID("");
						}

						((DoubleProp) property).setMinValue(annotation.min());
						((DoubleProp) property).setMaxValue(annotation.max());
						((DoubleProp) property).setEnableAuto(annotation.enableAuto());
						((DoubleProp) property).setEnableUnlimited(annotation.enableUnlimited());
						((DoubleProp) property).setStepSize(annotation.stepSize());
						((DoubleProp) property).setGuiElement(annotation.guiElement());

						this.getProperties().put(property.getPropertyID(), property);
						if ( !this.getLayerMapDisplayNameToConfigName().containsKey(layerDisplayName)){
							logger.log( Level.DEBUG , "Register plugin layer (" + layerConfigName + ", " + layerDisplayName + ")");
							logger.log( Level.DEBUG, "Set position for injected plugin layer " + layerConfigName + " to " + layerPosition);
							this.getLayerMapDisplayNameToConfigName().put(layerDisplayName, layerConfigName);
							this.layerMapConfigNameToDisplayName.put(layerConfigName, layerDisplayName);
							this.layerMapDisplayNameToOrder.put( layerDisplayName, layerPosition );
							this.layerMapConfigNameToOrder.put( layerConfigName, layerPosition );
							this.isStaticLayerMap.put(layerConfigName,property.isStatic());

						}
						else if ( !globalProperty  && !property.isStatic()){
							this.registerPlugin(pluginDisplayName, layerDisplayName, true);
						}
					}
				}

				if (element.annotationType() == StringSimulationProperty.class) {

					StringSimulationProperty annotation = field.getAnnotation(StringSimulationProperty.class);
					if ( !annotation.inject().equals("") ){

						InjectionParser injection = new InjectionParser( annotation.inject(), annotation.key() );

						String layerDisplayName = injection.getLayerDisplayName();
						String layerConfigName = injection.getLayerConfigName();
						String pluginDisplayName = injection.getPluginDisplayName();
						String pluginConfigName = injection.getPluginConfigName();
						int layerPosition = injection.getLayerPosition();
//						int pluginPosition = injection.getPluginPosition();

						property = new StringProp();
						property.setId(annotation.key());
						property.setName(annotation.name());
						property.setTooltip("Key: " + annotation.key() + "\n " + annotation.tooltip());
						property.setPluginLayerID(layerConfigName);
						property.setPluginID(pluginConfigName);
						property.setEnable_requirements(annotation.enable_requirements());
						property.setValue_requirements(annotation.value_requirements());
						property.setEnable(true);
						property.setInfo(annotation.info());
						property.setPosition(annotation.position());

						globalProperty = annotation.global() || injection.isGlobalProperty();
						property.setIsGlobal(globalProperty);
						property.setIsStatic(annotation.isStatic());
						property.isPropertyToVary(annotation.property_to_vary());

						if(property.isStatic()){
							property.setPluginID("");
						}

						// ((StringProp) property).setValue(annotation.value());
						String regex = "@\\w*";
						if (annotation.possibleValues().matches(regex)){
							((StringProp) property).setPossibleValues(possibleValueTargets.get("StatisticsType"));
						}else{
							((StringProp) property).setPossibleValues(annotation.possibleValues());
						}
						((StringProp) property).setMultiSelection(annotation.multiSelection());

						this.getProperties().put(property.getPropertyID(), property);

						if ( !this.getLayerMapDisplayNameToConfigName().containsKey(layerDisplayName)){
							logger.log( Level.DEBUG , "Register plugin layer (" + layerConfigName + ", " + layerDisplayName + ")");
							logger.log( Level.DEBUG, "Set position for injected plugin layer " + layerConfigName + " to " + layerPosition);
							this.getLayerMapDisplayNameToConfigName().put(layerDisplayName, layerConfigName);
							this.layerMapConfigNameToDisplayName.put(layerConfigName, layerDisplayName);
							this.layerMapDisplayNameToOrder.put( layerDisplayName, layerPosition );
							this.layerMapConfigNameToOrder.put( layerConfigName, layerPosition );
							this.isStaticLayerMap.put(layerConfigName,property.isStatic());

						} else if ( !globalProperty  && !property.isStatic()){
							this.registerPlugin(pluginDisplayName, layerDisplayName, true);
						}


					}
				}

			}
		}

		//		Field[] fieldArray = fields.toArray( new Field[fields.size()]);
		//		readFields(null, fieldArray, null, false);

	}

	/**
	 * Scans plugin dependent simulation properties
	 */
	@SuppressWarnings("unused")
	public void scanForPluginProperties() {

		SimGuiPlugin plugin;

		// TODO: Seems not to work properly, scans all packages
		Reflections reflectionsPlugins = new Reflections(
				ClasspathHelper.forPackage("userGeneratedContent.simulatorPlugIns"),
				new TypeAnnotationsScanner());

		// Look for classes with PluginAnnotation
		Set<Class<?>> types = reflectionsPlugins.getTypesAnnotatedWith(staticContent.evaluation.simulator.annotations.plugin.Plugin.class);

		for (Class<?> pluginClass : types) {

			Plugin pluginAnnotation = pluginClass.getAnnotation(Plugin.class);

			plugin = new SimGuiPlugin();
			plugin.setId(pluginClass.getName());
			plugin.setConfigName(pluginAnnotation.pluginKey());
			plugin.setDisplayName(pluginAnnotation.pluginName());
			plugin.setPluginLayer(pluginAnnotation.pluginLayerKey());
			plugin.isVisible(pluginAnnotation.visible());
			plugin.isGlobal(pluginAnnotation.global());
			plugin.allowGlobalFields(pluginAnnotation.allowGlobalFields());

			// This is the direct superclass. The direct superclass does not need
			// to be annotated with @PluginSuperclass. It also can be annotated with
			// @Plugin but then it must have the same name
			Class<?> directSuperlass = pluginClass.getSuperclass();

			// Find the annotated superclass
			Class<?> pluginSuperclass = plugin.getPluginSuperclass( pluginClass );

			// If there is a plugin layer provided by the plugin annotation, a plugin can
			// be registered drectly ( e.g. StopAndGoMessage.java ).
			// Otherwise the we try to find the pluginSuperclasse an use its name ( e.g. StopAndGo.java ).
			boolean autodetectPluginLayer = pluginAnnotation.pluginLayerKey().equals("");

			if ( !autodetectPluginLayer ){
				logger.log(Level.DEBUG, "Disable autodetect for plugin " + plugin.getConfigName() );

				// we need this for invisible plugins
				plugin.setFallbackLayer(pluginAnnotation.pluginLayerKey());

				// Although autodetection is disabled, it is a good idea to check if there is a superclass
				// with @PluginSuperclass annotation.
				// Reason: This superclass might have some properties!
				if ( pluginSuperclass != null ) {

					PluginSuperclass pluginSuperclassAnnotation = pluginSuperclass.getAnnotation( PluginSuperclass.class );

					// prefer pluginLayer from @PluginSuperclass annotation
					// let's overwrite it!
					String pluginSuperclassLayerKey = pluginSuperclassAnnotation.layerKey();
					String pluginSuperclassLayerName = pluginSuperclassAnnotation.layerName();
					plugin.setPluginLayer( pluginSuperclassLayerKey );

					// we need this for invisible plugins
					plugin.setFallbackLayer(pluginSuperclassLayerKey);

					// Process PluginSuperclass fields if not already done
					if ( !this.getLayerMapDisplayNameToConfigName().containsKey( pluginSuperclassLayerName )){

						// We can/must read PluginSuperclass fields without registering a plugin
						this.readFields( plugin, pluginSuperclass.getDeclaredFields(), pluginSuperclassLayerKey, true );
						this.getLayerMapDisplayNameToConfigName().put( pluginSuperclassLayerName, pluginSuperclassLayerKey);
						this.layerMapConfigNameToDisplayName.put( pluginSuperclassLayerKey, pluginSuperclassLayerName );
					}
				}

				// We have to defer the plugin registration, since now it is not clear
				// if there is another class of the same plugin that has a PluginSuperclass,
				// which has higher priority when setting the display name of the layer.
				this.deferedReadFields( plugin, pluginClass.getDeclaredFields(), pluginAnnotation.pluginLayerKey(), false );

				// Now it is safe to register the plugin and read the fields
				// registerPlugin( plugin.getName(), plugin.getPluginLayer(), plugin.isVisible() );
				// readFields( plugin, pluginClass.getDeclaredFields(), pluginAnnotation.pluginLayerKey(), false );

			}else{ // AUTODETECT PLUGINLAYER (Lookup superclass)
				logger.log(Level.DEBUG, "Enable autodetect for plugin " + plugin.getConfigName() + ", PluginSuperclass=" + pluginSuperclass.getSimpleName());

				String layerDisplayName = "";
				String layerConfigName = "";

				// Process PluginSuperclass information
				if ( pluginSuperclass != null ){

					PluginSuperclass pluginSuperclassAnnotation = pluginSuperclass.getAnnotation( PluginSuperclass.class );

					// Case: Invalid inheritance
					// If the direct superclass has a normal plugin annotation, the
					// provided name must match the actual plugin name.
					if (directSuperlass.isAnnotationPresent( Plugin.class ) &&
							(directSuperlass.getAnnotation( Plugin.class ).pluginKey() != pluginAnnotation.pluginKey()) ){

						logger.log( Level.ERROR , "Check the annotation in class " + directSuperlass.getCanonicalName() +
								" it should be @Plugin( name = \""+ plugin.getConfigName() +"\" ... ) or a valid @PluginSuperclass annotation.");
					}

					// Set the pluginLayer
					String pluginSuperclassLayerKey = pluginSuperclassAnnotation.layerKey();
					String pluginSuperclassLayerName = pluginSuperclassAnnotation.layerName();
					plugin.setPluginLayer( pluginSuperclassLayerKey );

					// we need this for invisible plugins
					plugin.setFallbackLayer(pluginSuperclassLayerKey);

					// Process PluginSuperclass fields if not already done
					if ( !this.getLayerMapDisplayNameToConfigName().containsKey( pluginSuperclassLayerName )){
						logger.log(Level.DEBUG, "Register PluginSuperclass for " + pluginSuperclassLayerKey);

						// register the plugin layer manually
						logger.log(Level.DEBUG, "Associate plugin layer (" +
								pluginSuperclassLayerKey + ", " + pluginSuperclassLayerName + ")");

						this.readFields( plugin, pluginSuperclass.getDeclaredFields(), pluginSuperclassLayerKey, true );
						this.getLayerMapDisplayNameToConfigName().put( pluginSuperclassLayerName, pluginSuperclassLayerKey);
						this.layerMapConfigNameToDisplayName.put( pluginSuperclassLayerKey, pluginSuperclassLayerName );

						int layerPosition = pluginSuperclass.getAnnotation( PluginSuperclass.class ).position();

						logger.log( Level.DEBUG, "Set position for plugin layer " + pluginSuperclassLayerKey + " to " + layerPosition);
						this.layerMapDisplayNameToOrder.put( pluginSuperclassLayerName, layerPosition );
						this.layerMapConfigNameToOrder.put( pluginSuperclassLayerKey, layerPosition );

						// PluginSuperclasses can provide fake plugins. Fake plugins will occur in the
						// jcombobox of the corresponding plugin level (see TopologyScript.java)
						String fakePlugins = pluginSuperclass.getAnnotation( PluginSuperclass.class ).fakePlugins();
						if ( !fakePlugins.equals("") ){
							String[] fakedPlugins = fakePlugins.split(",");
							for (String fakedPlugin : fakedPlugins) {
								String[] splitPlugin = fakedPlugin.split(":");
								
								SimGuiPlugin fakePlugin = new SimGuiPlugin();
								if ( splitPlugin.length >= 1){
									fakePlugin.setConfigName(splitPlugin[0]);
								}
								if ( splitPlugin.length > 1 ){
									fakePlugin.setDisplayName(splitPlugin[1]);
								}
								fakePlugin.setPluginLayer(pluginSuperclass.getAnnotation( PluginSuperclass.class ).layerKey());
								fakePlugin.isVisible(true);
								plugins.put(fakedPlugin, fakePlugin);
								
								logger.log(Level.DEBUG, "Inject fake plugin " + fakedPlugin + " to " + layerConfigName );
								this.registerPlugin(fakedPlugin, pluginSuperclass.getAnnotation( PluginSuperclass.class ).layerKey(), true);
							}
						}

					}else{
						logger.log(Level.DEBUG, plugin.getConfigName() + " is caped by a superclass " + pluginSuperclass.getAnnotation( PluginSuperclass.class ).layerKey());
						this.registerPlugin( plugin.getConfigName(), pluginSuperclass.getAnnotation( PluginSuperclass.class ).layerKey(), plugin.isVisible() );
					}
				}
				else{
					// The plugin is not O.K.! Each plugin must be caped by a pluginSuperclass
					logger.log(Level.ERROR, plugin.getConfigName() + " is not caped by a superclass");
					System.exit(-1);
				}

				PluginSuperclass pluginSuperclassAnnotation = pluginSuperclass.getAnnotation( PluginSuperclass.class );
				String pluginSuperclassLayerName = pluginSuperclassAnnotation.layerName();
				this.deferedReadFields( plugin, pluginClass.getDeclaredFields(), layerConfigName, false );
			}
			this.plugins.put(plugin.getConfigName(), plugin);
		}
		// call initial dependency-check for per plugin configurations
		DependencyChecker.checkAll(this);
	}


	/**
	 * Deferes the registration of simprops (fields of plugins).
	 * @param plugin is the corresponding plugin
	 * @param declaredFields are the fields of the class that is annotated as a plugin 
	 * @param pluginLayerKey is the id of the layer the plugin belongs to
	 * @param isSuperclass has to be true if the plugin registration was triggered by reading a pluginsuperclass annotation
	 */
	private void deferedReadFields(SimGuiPlugin plugin, Field[] declaredFields, String pluginLayerKey, boolean isSuperclass) {

		logger.log(Level.DEBUG, "Defer " + plugin.getConfigName() );

		Vector<Object> deferInformation = new Vector<Object>();
		deferInformation.add( plugin );
		deferInformation.add( declaredFields );
		deferInformation.add( pluginLayerKey);
		deferInformation.add( isSuperclass );

		this.deferList.add( deferInformation );
	}

	/**
	 * Processes the list of defered plugin registrations
	 */
	private void processDefered(){

		for ( Vector<Object> deferInformation : this.deferList ){

			SimGuiPlugin plugin = (SimGuiPlugin) deferInformation.get( 0 );
			Field[] declaredFields = (Field[]) deferInformation.get( 1 );
			String pluginLayerKey = (String) deferInformation.get( 2 );
			boolean isSuperclass = (boolean) deferInformation.get( 3 );

			this.registerPlugin( plugin.getConfigName(), plugin.getPluginLayer(), plugin.isVisible() );
			this.readFields( plugin, declaredFields, pluginLayerKey, isSuperclass );
		}

	}

	/**
	 * 
	 * Registers a plugin name with a plugin layer.
	 * 
	 * @param plugin A plugin name e.g.
	 * @param plugInLayer A player name e.g.
	 * @param isVisible A boolean which specifies whether the plugin name will be
	 * registered with a plugin layer (ture) or not (false). If a plugin is not
	 * registerd with a plugin layer, it will be invisible whithin the gui.
	 */
	private void registerPlugin(String plugin, String plugInLayer, boolean isVisible) {
		if ( isVisible ){

			GraphicsDevice graphicsDevice = GraphicsEnvironment
					.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			int x = graphicsDevice.getDisplayMode().getWidth();
			int y = graphicsDevice.getDisplayMode().getHeight();

			// Check if this plugin is already known for a different pluginlayer.
			// In this case show an alert an exit. The Pluginprogrammer has to fix ist
			if ( this.getRegisteredPlugins().containsKey(plugin) ){
				if ( !this.getRegisteredPlugins().get(plugin).equals(plugInLayer) ){
					JOptionPane alert = new JOptionPane("Reuse of Plugin name '" + plugin +
							"' in plugin layers: \n" +
							this.getRegisteredPlugins().get(plugin) + " \n" +
							plugInLayer + " \n" +
							"Please fix the problem!");

					JDialog dialog = alert.createDialog(null, "");
					int w = dialog.getWidth();
					int h = dialog.getHeight();
					dialog.setLocation((x / 2) - (w / 2), (y / 2) - (h / 2));
					dialog.setVisible(true);
					System.exit(-1);
				}

			}
			// Otherwise, register the plugin the pluginlayer.
			// Plugins which are registered to a pluginlayer will occur in the
			// pluginlayer's corresponging jcombobox!
			// Plugins which are not registered are not visible in the gui.
			else {
				logger.log(Level.DEBUG, "Register plugin (" + plugin + ", " + plugInLayer + ")");
				this.getRegisteredPlugins().put(plugin, plugInLayer);
			}
		} else{
			logger.log(Level.DEBUG, "Plugin (" + plugin + ", " + plugInLayer + ") is set to invisible");
		}

	}

	public Map<String, String> getRegisteredPlugins(){
		return this.registeredPlugins;
	}

	/**
	 * Registers the simprops (fields) for a plugin (as plugin annotated class) 
	 * @param plugin is the corresponding plugin
	 * @param declaredFields are the fields of the class that is annotated as a plugin 
	 * @param pluginLayerKey is the id of the layer the plugin belongs to
	 * @param isSuperclass has to be true if the plugin registration was triggered by reading a pluginsuperclass annotation
	 */
	private void readFields(SimGuiPlugin plugin, Field[] fields, String plugInLayer, boolean isSuperClass ) {

		// Skip invisible plugins
		if ( !plugin.isVisible() && !(plugin.isGlobal() || plugin.allowGlobalFields()) ){
			logger.log(Level.DEBUG,
					plugin.getConfigName() + " is ignored due to isVisible=" +
							plugin.isVisible()	+ " and makeFieldsGlobal=" +
							plugin.isGlobal() + " and allowGlobalFields=" + plugin.allowGlobalFields());
			return;
		}

		SimProp property;

		try {
			for (Field field : fields ) {

				Annotation[] a = field.getAnnotations();
				for (Annotation element : a) {

					boolean isGlobal = false;
					if (element.annotationType() == BoolSimulationProperty.class) {
						BoolSimulationProperty annotation = field.getAnnotation(BoolSimulationProperty.class);
						property = new BoolProp();
						if (annotation != null) {
							if ( !annotation.inject().equals("") ){
								logger.log(Level.DEBUG, "Skip " + annotation.key() + " from " + plugin.getConfigName() + " has injection annotation");
								continue;
							}

							// Uninjected properties are global under following conditions:
							// They are PluginSuperclass properties OR the if plugin allows global Properties.
							// If the plugin allows global properties each property which is annotated as global
							// will become a global plugin layer property ( e.g. LineChartPlotterCf.java )
							isGlobal = isSuperClass || ( plugin.isGlobal() || annotation.global() );

							property.setId(annotation.key());
							if ( !isGlobal ){
								property.setPluginID(plugin.getConfigName());
								property.setIsGlobal( false );
							}else{
								property.setPluginID("");
								property.setIsGlobal( true );
							}
							logger.log(Level.DEBUG, annotation.key() + " is global=" +
									property.isGlobal() + " so plugin is " + property.getPluginID() );

							property.setIsSuperclass( isSuperClass );
							property.setPluginLayerID(plugInLayer);

							property.setName(annotation.name());
							property.setTooltip("Key: " + annotation.key() + "\n " + annotation.tooltip());
							property.setInfo(annotation.info());
							property.setPosition(annotation.position());
							
							property.isPropertyToVary(annotation.property_to_vary());

							// This is why we have to defer all readFields() calls
							// Explanation: We call getPluginLayer() but the internal list which is used by this
							// function builds up dynamically. Therefore, it is possible to get an empty string or
							// null, if we have bad timing!
							// CAUTION: do not touch these expressions, we make use of lazy evaluation
							String possiblePluginLayer = this.getPluginLayer(plugin.getConfigName());
							if ( (possiblePluginLayer != null) && !possiblePluginLayer.isEmpty() ){
								property.setPluginLayerID(this.getPluginLayer(plugin.getConfigName()));
							}else{
								if ( !plugin.getFallbackLayer().isEmpty() ){
									logger.log(Level.DEBUG, "Fallback to " + plugin.getFallbackLayer());
									property.setPluginLayerID(plugin.getFallbackLayer());
								}else{
									logger.log(Level.ERROR, "Can not determine the Layer for " + property.getName() + "! Reason unknown");
									System.exit(-1);
								}
							}

							logger.log(Level.DEBUG, annotation.key() + " set layer to " + this.getPluginLayer(plugin.getConfigName()));

							property.setEnable_requirements(annotation.enable_requirements());
							property.setValue_requirements(annotation.value_requirements());
							property.setEnable(true);

							// ((BoolProp) property).setValue(annotation.value());
						}
						this.register(property, isSuperClass, isGlobal, plugInLayer);
					} else if (element.annotationType() == IntSimulationProperty.class) {
						IntSimulationProperty annotation = field
								.getAnnotation(IntSimulationProperty.class);
						property = new IntProp();
						if (annotation != null) {
							if ( !annotation.inject().equals("") ){
								logger.log(Level.DEBUG, "Skip " + annotation.key() + " from " + plugin.getConfigName() + " has injection annotation");
								continue;
							}

							// Uninjected properties are global under following conditions:
							// They are PluginSuperclass properties OR the if plugin allows global Properties.
							// If the plugin allows global properties each property which is annotated as global
							// will become a global plugin layer property ( e.g. LineChartPlotterCf.java )
							isGlobal = isSuperClass || ( plugin.isGlobal() || annotation.global() );

							property.setId(annotation.key());
							if ( !isGlobal ){
								property.setPluginID(plugin.getConfigName());
								property.setIsGlobal( false );
							}else{
								property.setPluginID("");
								property.setIsGlobal( true );
							}
							logger.log(Level.DEBUG, annotation.key() + " is global=" +
									property.isGlobal() + " so plugin is " + property.getPluginID() );

							property.setIsSuperclass( isSuperClass );
							property.setPluginLayerID(plugInLayer);

							property.setName(annotation.name());
							property.setTooltip("Key: " + annotation.key() + ", " + annotation.tooltip());
							property.setInfo(annotation.info());
							property.setPosition(annotation.position());
							
							property.isPropertyToVary(annotation.property_to_vary());

							// This is why we have to defer all readFields() calls
							// Explanation: We call getPluginLayer() but the internal list which is used by this
							// function builds up dynamically. Therefore, it is possible to get an empty string or
							// null, if we have bad timing!
							// CAUTION: do not touch these expressions, we make use of lazy evaluation
							String possiblePluginLayer = this.getPluginLayer(plugin.getConfigName());
							if ( (possiblePluginLayer != null) && !possiblePluginLayer.isEmpty() ){
								property.setPluginLayerID(this.getPluginLayer(plugin.getConfigName()));
							}else{
								if ( !plugin.getFallbackLayer().isEmpty() ){
									logger.log(Level.DEBUG, "Fallback to " + plugin.getFallbackLayer());
									property.setPluginLayerID(plugin.getFallbackLayer());
								}else{
									logger.log(Level.ERROR, "Can not determine the Layer for " + property.getName() + "! Reason unknown");
									System.exit(-1);
								}
							}

							logger.log(Level.DEBUG, annotation.key() + " set layer to " + this.getPluginLayer(plugin.getConfigName()));

							property.setEnable_requirements(annotation.enable_requirements());
							property.setValue_requirements(annotation.value_requirements());
							property.setEnable(true);

							((IntProp) property).setMinValue(annotation.min());
							((IntProp) property).setMaxValue(annotation.max());
							((IntProp) property).setEnableAuto(annotation.enableAuto());
							((IntProp) property).setEnableUnlimited(annotation.enableUnlimited());
							((IntProp) property).setStepSize(annotation.stepSize());
							((IntProp) property).setGuiElement(annotation.guiElement());
						}
						this.register(property, isSuperClass, isGlobal, plugInLayer);
					} else if (element.annotationType() == FloatSimulationProperty.class) {
						FloatSimulationProperty annotation = field
								.getAnnotation(FloatSimulationProperty.class);
						property = new FloatProp();
						if (annotation != null) {
							if ( !annotation.inject().equals("") ){
								logger.log(Level.DEBUG, "Skip " + annotation.key() + " from " + plugin.getConfigName() + " has injection annotation");
								continue;
							}

							// Uninjected properties are global under following conditions:
							// They are PluginSuperclass properties OR the if plugin allows global Properties.
							// If the plugin allows global properties each property which is annotated as global
							// will become a global plugin layer property ( e.g. LineChartPlotterCf.java )
							isGlobal = isSuperClass || ( plugin.isGlobal() || annotation.global() );

							property.setId(annotation.key());
							if ( !isGlobal ){
								property.setPluginID(plugin.getConfigName());
								property.setIsGlobal( false );
							}else{
								property.setPluginID("");
								property.setIsGlobal( true );
							}
							logger.log(Level.DEBUG, annotation.key() + " is global=" +
									property.isGlobal() + " so plugin is " + property.getPluginID() );

							property.setIsSuperclass( isSuperClass );
							property.setPluginLayerID(plugInLayer);

							property.setName(annotation.name());
							property.setTooltip("Key: " + annotation.key() + ", " + annotation.tooltip());
							property.setInfo(annotation.info());
							property.setPosition(annotation.position());
							
							property.isPropertyToVary(annotation.property_to_vary());

							// This is why we have to defer all readFields() calls
							// Explanation: We call getPluginLayer() but the internal list which is used by this
							// function builds up dynamically. Therefore, it is possible to get an empty string or
							// null, if we have bad timing!
							// CAUTION: do not touch these expressions, we make use of lazy evaluation
							String possiblePluginLayer = this.getPluginLayer(plugin.getConfigName());
							if ( (possiblePluginLayer != null) && !possiblePluginLayer.isEmpty() ){
								property.setPluginLayerID(this.getPluginLayer(plugin.getConfigName()));
							}else{
								if ( !plugin.getFallbackLayer().isEmpty() ){
									logger.log(Level.DEBUG, "Fallback to " + plugin.getFallbackLayer());
									property.setPluginLayerID(plugin.getFallbackLayer());
								}else{
									logger.log(Level.ERROR, "Can not determine the Layer for " + property.getName() + "! Reason unknown");
									System.exit(-1);
								}
							}

							logger.log(Level.DEBUG, annotation.key() + " set layer to " + this.getPluginLayer(plugin.getConfigName()));

							property.setEnable_requirements(annotation.enable_requirements());
							property.setValue_requirements(annotation.value_requirements());
							property.setEnable(true);

							((FloatProp) property).setMinValue(annotation.min());
							((FloatProp) property).setMaxValue(annotation.max());
							((FloatProp) property).setEnableAuto(annotation.enableAuto());
							((FloatProp) property).setEnableUnlimited(annotation.enableUnlimited());
							((FloatProp) property).setStepSize(annotation.stepSize());
							((FloatProp) property).setGuiElement(annotation.guiElement());
						}
						this.register(property, isSuperClass, isGlobal, plugInLayer);
					} else if (element.annotationType() == DoubleSimulationProperty.class) {
						DoubleSimulationProperty annotation = field
								.getAnnotation(DoubleSimulationProperty.class);
						property = new DoubleProp();
						if (annotation != null) {
							if ( !annotation.inject().equals("") ){
								logger.log(Level.DEBUG, "Skip " + annotation.key() + " from " + plugin.getConfigName() + " has injection annotation");
								continue;
							}

							// Uninjected properties are global under following conditions:
							// They are PluginSuperclass properties OR the if plugin allows global Properties.
							// If the plugin allows global properties each property which is annotated as global
							// will become a global plugin layer property ( e.g. LineChartPlotterCf.java )
							isGlobal = isSuperClass || ( plugin.isGlobal() || annotation.global() );

							property.setId(annotation.key());
							if ( !isGlobal ){
								property.setPluginID(plugin.getConfigName());
								property.setIsGlobal( false );
							}else{
								property.setPluginID("");
								property.setIsGlobal( true );
							}
							logger.log(Level.DEBUG, annotation.key() + " is global=" +
									property.isGlobal() + " so plugin is " + property.getPluginID() );

							property.setIsSuperclass( isSuperClass );
							property.setPluginLayerID(plugInLayer);

							property.setName(annotation.name());
							property.setTooltip("Key: " + annotation.key() + ", " + annotation.tooltip());
							property.setInfo(annotation.info());
							property.setPosition(annotation.position());
							
							property.isPropertyToVary(annotation.property_to_vary());

							// This is why we have to defer all readFields() calls
							// Explanation: We call getPluginLayer() but the internal list which is used by this
							// function builds up dynamically. Therefore, it is possible to get an empty string or
							// null, if we have bad timing!
							// CAUTION: do not touch these expressions, we make use of lazy evaluation
							String possiblePluginLayer = this.getPluginLayer(plugin.getConfigName());
							if ( (possiblePluginLayer != null) && !possiblePluginLayer.isEmpty() ){
								property.setPluginLayerID(this.getPluginLayer(plugin.getConfigName()));
							}else{
								if ( !plugin.getFallbackLayer().isEmpty() ){
									logger.log(Level.DEBUG, "Fallback to " + plugin.getFallbackLayer());
									property.setPluginLayerID(plugin.getFallbackLayer());
								}else{
									logger.log(Level.ERROR, "Can not determine the Layer for " + property.getName() + "! Reason unknown");
									System.exit(-1);
								}
							}

							logger.log(Level.DEBUG, annotation.key() + " set layer to " + this.getPluginLayer(plugin.getConfigName()));

							property.setEnable_requirements(annotation.enable_requirements());
							property.setValue_requirements(annotation.value_requirements());
							property.setEnable(true);

							((DoubleProp) property).setMinValue(annotation.min());
							((DoubleProp) property).setMaxValue(annotation.max());
							((DoubleProp) property).setEnableAuto(annotation.enableAuto());
							((DoubleProp) property).setEnableUnlimited(annotation.enableUnlimited());
							((DoubleProp) property).setStepSize(annotation.stepSize());
							((DoubleProp) property).setGuiElement(annotation.guiElement());
						}
						this.register(property, isSuperClass, isGlobal, plugInLayer);
					} else if (element.annotationType() == StringSimulationProperty.class) {
						StringSimulationProperty annotation = field
								.getAnnotation(StringSimulationProperty.class);
						property = new StringProp();
						if (annotation != null) {
							if ( !annotation.inject().equals("") ){
								logger.log(Level.DEBUG, "Skip " + annotation.key() + " from " + plugin.getConfigName() + " has injection annotation");
								continue;
							}

							// Uninjected properties are global under following conditions:
							// 1. The property is a PluginSuperclass property.
							// The corresponding plugin is global.
							// The property itself is global ( e.g. LineChartPlotterCf.java )
							isGlobal = isSuperClass || ( plugin.isGlobal() || annotation.global() );

							property.setId(annotation.key());
							if ( !isGlobal ){
								property.setPluginID(plugin.getConfigName());
								property.setIsGlobal( false );
							}else{
								property.setPluginID("");
								property.setIsGlobal( true );
							}
							logger.log(Level.DEBUG, annotation.key() + " is global=" +
									property.isGlobal() + " so plugin is " + property.getPluginID() );

							property.setIsSuperclass( isSuperClass );
							property.setPluginLayerID(plugInLayer);

							property.setName(annotation.name());
							property.setTooltip("Key: " + annotation.key() + ", " + annotation.tooltip());
							property.setInfo(annotation.info());
							property.setPosition(annotation.position());
							
							property.isPropertyToVary(annotation.property_to_vary());

							// This is why we have to defer all readFields() calls
							// Explanation: We call getPluginLayer() but the internal list which is used by this
							// function builds up dynamically. Therefore, it is possible to get an empty string or
							// null, if we have bad timing!
							// CAUTION: do not touch these expressions, we make use of lazy evaluation
							String possiblePluginLayer = this.getPluginLayer(plugin.getConfigName());
							if ( (possiblePluginLayer != null) && !possiblePluginLayer.isEmpty() ){
								property.setPluginLayerID(this.getPluginLayer(plugin.getConfigName()));
							}else{
								if ( !plugin.getFallbackLayer().isEmpty() ){
									logger.log(Level.DEBUG, "Fallback to " + plugin.getFallbackLayer());
									property.setPluginLayerID(plugin.getFallbackLayer());
								}else{
									logger.log(Level.ERROR, "Can not determine the Layer for " + property.getName() + "! Reason unknown");
									System.exit(-1);
								}
							}

							logger.log(Level.DEBUG, annotation.key() + " set layer to " + this.getPluginLayer(plugin.getConfigName()));

							property.setEnable_requirements(annotation.enable_requirements());
							property.setValue_requirements(annotation.value_requirements());
							property.setEnable(true);

							((StringProp) property).setPossibleValues(annotation.possibleValues());
							((StringProp) property).setMultiSelection(annotation.multiSelection());
						}
						this.register(property, isSuperClass, isGlobal, plugInLayer);
					} else {
						logger.log(Level.ERROR, this + "Bad property type for field" + field.getName() );
						continue;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Looks up the mappingbetween plugin names and layer names
	 * @param pluginName id of a plugin
	 * @return The name of the layer the plugin belongs to
	 */
	private String getPluginLayer(String pluginName) {

		if ( this.getRegisteredPlugins().containsKey(pluginName) ){
			return this.getRegisteredPlugins().get(pluginName);
		}
		logger.log(Level.ERROR, "Plugin " + pluginName + " is unknown");
		return null;
	}

	/**
	 * Scans the registered plugins and build up some mappings
	 */
	public void scanPlugins() {

		// Get the display names of plugin layers
		String[] pluginLayer = this.layerMapConfigNameToDisplayName.keySet().toArray(new String[0]);
		for (int i = 0; i < this.layerMapConfigNameToDisplayName.size(); i++) {
			this.pluginLayerMap[i] = new HashMap<String, String>();
		}

		for ( Entry<String, String> entry : this.getRegisteredPlugins().entrySet() ) {

			String name = entry.getKey();
			String layer = entry.getValue();

			boolean insertFlag = false;
			for (int i = 0; i < this.getLayerMapDisplayNameToConfigName().size(); i++){

				// Append plugins config name and corresponding layers display name
				// if plugin's layer matches
				if ( layer.equals(pluginLayer[i]) ){
					this.pluginLayerMap[i].put( name, layer );
					insertFlag = true;
					this.numberOfPluginLayers = Math.max(this.numberOfPluginLayers, i+1);
				}
			}

			if ( !insertFlag ) {
				logger.log(Level.ERROR, "No such plugin layer: " + layer );
			}
		}
	}

	/**
	 * Sets the value of a simprop
	 * @param key identifier of a simprop
	 * @param arg0 thevalue
	 */
	public void setValue(String key, Object arg0) {

		logger.log(Level.DEBUG, "Set " + key + " to " + arg0);

		if (arg0.getClass() == Boolean.class) {
			this.getProperties().get(key).setValue(arg0);
		} else if (arg0.getClass() == Float.class) {
			this.getProperties().get(key).setValue(arg0);
		} else if (arg0.getClass() == Double.class) {
			this.getProperties().get(key).setValue(arg0);
		}else if (arg0.getClass() == Integer.class) {
			this.getProperties().get(key).setValue(arg0);
		} else if (arg0.getClass() == String.class) {
			this.getProperties().get(key).setValue(arg0);
		} else {

		}
		setUnsavedChanges(true);
		DependencyChecker.checkAll(getInstance());
	}

	/**
	 * Can be used to get a list of simprops that belog to a specific layer
	 * @param pluginLayer is the id of a layer
	 * @return a list of simprops that belog to the specified layer
	 */
	public List<SimProp> getGlobalSimPropertiesByPluginLayer(String pluginLayer) {

		// TODO: order simproperties here

		List<SimProp> simPropsInPluginLayer = new LinkedList<SimProp>();

		for ( String key : this.getProperties().keySet() ) {
			SimProp simProp = this.getProperties().get(key);

			if ( ( simProp.isSuperclass() || simProp.isGlobal() )
					&& simProp.getPluginLayerID().equals(this.getLayerMapDisplayNameToConfigName().get(pluginLayer)
							) ){

				simPropsInPluginLayer.add( simProp );
			}
		}

		return simPropsInPluginLayer;
	}

	/**
	 * Can be used to get a list of simprops that belog to a specific layer or plugin
	 * @param pluginName is the id of a plugin
	 * @param pluginLayer is the id of a layer
	 * @return a list of simprops that belog to the specified layer or plugin
	 */
	public List<SimProp> getSimPropertiesByPluginOrPluginLayer(String pluginName, String pluginLayer) {

		// TODO: order simproperties here

		List<SimProp> simPropertiesInANamespace = new LinkedList<SimProp>();

		for ( String key : this.getProperties().keySet() ) {
			SimProp simProp = this.getProperties().get(key);

			if ( simProp.getPluginID().equals(pluginName) ||
					( simProp.isSuperclass() && simProp.getPluginLayerID().equals(
							this.getLayerMapDisplayNameToConfigName().get(pluginLayer)
							)) ){
				simPropertiesInANamespace.add( simProp );
			}
		}

		return simPropertiesInANamespace;
	}

	/**
	 * Sets a specific plugin to active (visible in the gui - will be written to configuration)
	 * @param layerDisplayName is the layer the plugin belongs to
	 * @param selectedPlugin is the plugin that should be activated
	 */
	public void setActivePlugins(String layerDisplayName, String selectedPlugin) {
		logger.log(Level.DEBUG, "setActivePlugins " + layerDisplayName + " plugin to " + selectedPlugin);
		this.activePlugins.put(layerDisplayName, selectedPlugin);
		this.activePluginsMapped.put( this.displayNameToConfigName(layerDisplayName), selectedPlugin);
		this.setUnsavedChanges(true);
	}

	/**
	 * Sets a specific plugin to active (visible in the gui - will be written to configuration)
	 * @param layerConfigName is the layer the plugin belongs to
	 * @param selectedPlugin is the plugin that should be activated
	 */
	public void setActivePluginsMapped(String layerConfigName, String selectedPlugin) {
		logger.log(Level.DEBUG, "setActivePluginsMapped " + layerConfigName + " plugin to " + selectedPlugin);
		this.activePlugins.put(this.configNameToDisplayName(layerConfigName), selectedPlugin);
		this.activePluginsMapped.put(layerConfigName, selectedPlugin);
	}

	/**
	 * Used to obtain a mapping from layer to active plugin
	 * @param mapped indicates if the the key (layer) should be the config name (true) or the display name (false)
	 * @return mapping from layer to active plugin
	 */
	public Map<String, String> getActivePlugins(boolean mapped) {
		if (mapped){
			return this.activePluginsMapped;
		}else{
			return this.activePlugins;
		}
	}

	// TODO: remove this peace of code
	/**
	 * Used to obain a list of all layers
	 * @return a list of all layers
	 */
	public List<String> getPluginLayers() {
		return new LinkedList<String>(this.getLayerMapDisplayNameToConfigName().keySet());
	}

	// TODO: remove this peace of code
	/**
	 * Translates layer display names to layer config names 
	 * @param is display name (gui) of a layer
	 * @return the config name (configuration) of a layer
	 */
	public String displayNameToConfigName(String displayName) {
		return this.getLayerMapDisplayNameToConfigName().get(displayName);
	}

	/**
	 * Translates layer config names to layer display names 
	 * @param is config name (configuration) of a layer
	 * @return the display name (gui) of a layer
	 */
	public String configNameToDisplayName(String configName) {
		return this.layerMapConfigNameToDisplayName.get(configName);
	}

	/**
	 * Used to obtain a list of all known config name of layers
	 * @return The list of all layers config names 
	 */
	public List<String> getConfigNamesForPluginLayers() {

		List<String> configNamesForPluginLayers = new LinkedList<String>();
		for ( Entry<String, String> entry : this.layerMapConfigNameToDisplayName.entrySet() )
		{
			configNamesForPluginLayers.add( entry.getKey() );
		}
		return configNamesForPluginLayers;
	}

	@Override
	public String toString(){

		String tree = "";
		for ( String layer : this.getLayerMapDisplayNameToConfigName().keySet() ){
			for ( String prop : this.getProperties().keySet() ){
				if ( this.getProperties().get(prop).getPluginID().equals("") &&
						( this.getProperties().get(prop).isSuperclass() || this.getProperties().get(prop).isGlobal() ) &&
						this.getProperties().get(prop).getPluginLayerID().equals(this.getLayerMapDisplayNameToConfigName().get(layer)) ){

					// injected to plugin configuration
				}
			}
			for ( String plugin : this.getRegisteredPlugins().keySet() ){
				if ( this.getRegisteredPlugins().get(plugin).equals(this.getLayerMapDisplayNameToConfigName().get(layer))){

					// plugins
					for ( String prop : this.getProperties().keySet() ){
						if ( this.getProperties().get(prop).getPluginID().equals(plugin) ){

							// plugin dependent properties
						}
					}
				}
			}
		}

		return tree;
	}
	
	/**
	 * Used to look up all plugins within a specific layer
	 * @param pluginLayer is the id of the layer
	 * @return A list of the plugins which belong to pluginLayer
	 */
	public Map<String, String> getPluginsInLayer( String pluginLayer ) {
		Map<String, String> tmp1 = new HashMap<String, String>();

		for ( String layer : this.getLayerMapDisplayNameToConfigName().keySet() ){
			if ( layer.equals( pluginLayer )){
				
				for (SimGuiPlugin plugin : this.plugins.values() ){
					if ( plugin.getPluginLayer().equals(this.layerMapDisplayNameToConfigName.get(pluginLayer)) && plugin.isVisible() ){
						tmp1.put(plugin.getDisplayName(), plugin.getConfigName());
					}
				}
			}
		}
		return tmp1;
	}
	

	public Map<String, Integer> getStaticConfigurationDisplay() {
		return this.staticConfigurationDisplay;
	}

	public Map<String, SimGuiPlugin> getPluginMap() {
		return this.plugins;
	}

	public Map<String, String> getLayerMapDisplayNameToConfigName() {
		return this.layerMapDisplayNameToConfigName;
	}

	public Map<String, SimProp> getProperties() {
		return this.properties;
	}
	
	/**
	 * Used to look up simprops by their display names
	 * @param name is a display name of a simprop
	 * @return a first simporp that matches the name (this is dangerous since different simprops may have the same display name) 
	 */
	public SimProp getPropertiesByName(String name) {
		
		for (SimProp simProp : this.properties.values() ){
			if ( simProp.getName().equals(name)){
				logger.log(Level.DEBUG, "found property " + name + " -> " + simProp.getPropertyID());
				return simProp;
			}
		}
		
		logger.log(Level.DEBUG, "No such property " + name);
		return null;
	}
	
	/**
	 * Used to look up simprops by their id
	 * @param id is the id of a simprop
	 * @return the simporp that matches the id (much better than getPropertiesByName) 
	 */
	public String getPropertieNameByID(String id) {
		
		for (SimProp simProp : this.properties.values() ){
			if ( simProp.getPropertyID().equals(id)){				
				return simProp.getName();
			}			
		}
		return "---";		
	}

	/**
	 * Enables auto checkbox for a simprop
	 * @param id is the id of the simprop
	 * @param auto specifies wheter the checkbox will be enabled (true) or disables (false)
	 * @param c is the class of the simprop (Float, Double or Integer)
	 */
	public void setAuto(String id, boolean auto, Class<?> c) {
		
		if (c == Float.class) {
			((FloatProp) this.getProperties().get(id)).setAuto(auto);
		} else if (c == Double.class) {
			((DoubleProp) this.getProperties().get(id)).setAuto(auto);
		}else if (c == Integer.class) {
			((IntProp) this.getProperties().get(id)).setAuto(auto);
		} else {

		}
	}

	/**
	 * Enables unlimited checkbox for a simprop
	 * @param id is the id of the simprop
	 * @param unlimited specifies wheter the checkbox will be enabled (true) or disables (false)
	 * @param c is the class of the simprop (Float, Double or Integer)
	 */
	public void setUnlimited(String key, boolean unlimited, Class<?> c) {

		if (c == Float.class) {
			((FloatProp) this.getProperties().get(key)).setUnlimited(unlimited);
		} else if (c == Double.class) {
			((DoubleProp) this.getProperties().get(key)).setUnlimited(unlimited);
		}else if (c == Integer.class) {
			((IntProp) this.getProperties().get(key)).setUnlimited(unlimited);
		} else {

		}
	}

	/**
	 * Registers a gui element on a simprops as an observer
	 * @param guiElement the observer
	 * @param propertyID the simprop id
	 */
	public void registerGuiElement(Observer guiElement, String propertyID) {
		for ( Entry<String, SimProp> entry : this.properties.entrySet() ){
			if (entry.getValue().getPropertyID().equals(propertyID)) {
				entry.getValue().register( guiElement );
			}
		}
	}

	/**
	 * Looks up the plugin display name given the plugin config name
	 * @param pluginConfigName is the config name
	 * @return The plugin display name
	 */
	public String getPluginDisplayName(String pluginConfigName) {
		for ( SimGuiPlugin simPlugin : this.plugins.values() ){
			if ( simPlugin.getConfigName().equals(pluginConfigName) )
				return simPlugin.getDisplayName();
		}
		
		return pluginConfigName;	
	}

	/**
	 * Sets property to vary values
	 * @param key is the key of the property
	 * @param value is the new value of the property 
	 */
	public void setPropertyToVaryValue(String key, String value) {
		propertiesToVary.put(key, value);		
	}

	/**
	 * @return The map of properties which should be varied
	 */
	public Map<String, String> getPropertiesToVary() {
		return propertiesToVary;
	}

	public String getCurrentConfigFile() {
		return currentConfigFile;
	}

	public void setCurrentConfigFile(String currentConfigFile) {
		SimPropRegistry.currentConfigFile = currentConfigFile;
	}

	public Boolean getUnsavedChanges() {
		return unsavedChanges;
	}

	public void setUnsavedChanges(Boolean unsavedChanges) {
		this.unsavedChanges = unsavedChanges;
	}
}
