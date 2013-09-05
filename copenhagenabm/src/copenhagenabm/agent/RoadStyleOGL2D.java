package copenhagenabm.agent;

import java.awt.Color;

import copenhagenabm.environment.Road;

import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;

public class RoadStyleOGL2D extends DefaultStyleOGL2D {
    @Override
    public Color getColor(final Object agent) {
            if (agent instanceof Road) {
//                    final Road bug = (Road) agent;

                    final int strength = (int) Math.max(200 - 20 *1,100);
                    return new Color(0xFF, strength, strength); // 0xFFFFFF - white,
                                                                // 0xFF0000 - red
            }

            return super.getColor(agent);
    }
   
}