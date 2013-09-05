package copenhagenabm.tests;

import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.SimpleAdder;
import repastcity3.environment.contexts.RoadContext;
import copenhagenabm.environment.Road;
import copenhagenabm.main.GlobalVars;

public class TestGeography {

	private RoadContext roadContext;
	private Geography<Road> roadProjection;

	private void testG() {
		roadContext = new RoadContext();
		roadProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography(
				GlobalVars.CONTEXT_NAMES.ROAD_GEOGRAPHY, roadContext,
				new GeographyParameters<Road>(new SimpleAdder<Road>()));
		
		System.out.println(roadProjection);
	}
	
	public static void main(String[] args) {
		TestGeography tg = new TestGeography();
		tg.testG();

	}
	
}
