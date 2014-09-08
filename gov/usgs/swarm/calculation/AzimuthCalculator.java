package gov.usgs.swarm.calculation;


/**
 * Computes azimuth calculations required in determining events while markers are been placed.
 * 
 * @author Joel Shellman
 */
public class AzimuthCalculator {
    private static final double RADDEG = 57.2958;
    private static final double PSRAT = 1.73;

    private double rmsamp;
    private double azimuth;
    private double coherence;
    private double velocity;


    /**
     * @param dataMatrix       data matrix :
     *                         [0, *] - vertical channel (positive direction up)
     *                         [1, *] - north channel
     *                         [2, *] - east channel
     * @param firstSampleIndex first sample in time interval
     * @param lastSampleIndex  last sample in time interval
     * @param pvel             local p-wave velocity at site
     * @return
     */
    public double calculate(double[][] dataMatrix, int firstSampleIndex, int lastSampleIndex, double pvel) {
        if (dataMatrix == null || firstSampleIndex > lastSampleIndex) {
            throw new IllegalArgumentException("Wrong data window specified");
        }
        double[] dc = new double[3];
        for (int i = firstSampleIndex; i < lastSampleIndex; i++) {
            for (int l = 0; l < 3; l++) {
                dc[l] += dataMatrix[l][i];
            }
        }
        for (int i = 0; i < 3; i++) {
            dc[i] = dc[i] / (lastSampleIndex - firstSampleIndex + 1);
            for (int l = firstSampleIndex; l < lastSampleIndex; l++) {
                dataMatrix[i][l] -= dc[i];
            }
        }

        // Model 8 - p waves only
        //Calculate the auto and cross correlations.
        int lenwin = lastSampleIndex - firstSampleIndex + 1;
        double xx = cross(dataMatrix[1], dataMatrix[1], firstSampleIndex, lastSampleIndex);
        double xz = cross(dataMatrix[1], dataMatrix[0], firstSampleIndex, lastSampleIndex);
        double yy = cross(dataMatrix[2], dataMatrix[2], firstSampleIndex, lastSampleIndex);
        double yz = cross(dataMatrix[2], dataMatrix[0], firstSampleIndex, lastSampleIndex);
        double zz = cross(dataMatrix[0], dataMatrix[0], firstSampleIndex, lastSampleIndex);
        
        double power = (xx + yy + zz) / lenwin;
        rmsamp = Math.sqrt(power);

        // Calculate azimuth and vertical/radial amplitude ratio
        double azi = Math.atan2(-yz, -xz);
        azimuth = azi * RADDEG;
        if (azimuth < 0) {
            azimuth += 360;
        }
        double zoverr = (zz/1.0E5) / Math.sqrt((xz/1.0E5) * (xz/1.0E5) + (yz/1.0E5) * (yz/1.0E5));
        double a = -zoverr * Math.cos(azi);
        double b = -zoverr * Math.sin(azi);

        // Calculate predicted coherence
        double err = 0;
        for (int i = firstSampleIndex; i < lastSampleIndex; i++) {
            double cc = dataMatrix[0][i] - a * dataMatrix[1][i] - b * dataMatrix[2][i];
            err += cc * cc;
        }

        coherence = 1.0 - err / zz;

        // Simple biased velocity estimation (to obtain an unbiased estimate one need to know the noise amplitude)

//        SVEL=PVEL/PSRAT
//        AI=ATAN(1./ZOVERR)
//        VELO=SVEL/SIN(AI/2.)
        double svel = pvel / PSRAT;
        double ai = Math.atan(1.0 / zoverr);
        velocity = svel / Math.sin(ai / 2.0);

        return azimuth;
    }
    
    /**
     * Get root mean square amplitude. calculate method needs to be called first.
     * @return amplitude
     */
    public double getRootMeanSquareAmplitude() {
        return rmsamp;
    }

    /**
     * Get P-phase azimuth (towards event) in degrees. calculate method needs to be called first.
     * @return azimuth
     */
    public double getAzimuth() {
        return azimuth;
    }

    /**
     * Get predicted coherence, should be positive and larger than about 0.1 for P-phase. calculate method needs to be called first.
     * @return coherence
     */
    public double getCoherence() {
        return coherence;
    }

    /**
     * Get P-wave apparent velocity in km/sec. calculate method needs to be called first.
     * @return
     */
    public double getVelocity() {
        return velocity;
    }

    /**
     * Calculates unnormalised cross correlation between the two real time series x and y of length l.
     *
     * @param x
     * @param y
     * @param firstSampleIndex
     * @param lastSampleIndex
     * @return
     */
    private static double cross(double[] x, double[] y, int firstSampleIndex, int lastSampleIndex) {
        double a = 0;
        for (int i = firstSampleIndex; i < lastSampleIndex; i++) {
            a += x[i] * y[i];
        }
        return a;
    }

}
