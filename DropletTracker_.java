import ij.plugin.filter.PlugInFilter;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.lang.Float;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.process.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.Analyzer;
import ij.measure.*;
 

/**
  * DropletTracker: An ImageJ plugin for tracking microfluidic
  * droplets with microscope imaging.
  * Travis Geis, SGTC, Stanford University, July 2013

  * DropletTracker is based on MTrack2, 
  * by Nico Stuurman, Vale Lab, UCSF/HHMI, May,June 2003
    
  * MTrack2 is in turn based on Based on Multitracker,
  * based on the Object Tracker plugin filter by Wayne Rasband
  */

public class DropletTracker_ implements PlugInFilter, Measurements  {

    ImagePlus   imp;
    Calibration cal;
    int     nParticles;
    float[][]   ssx;
    float[][]   ssy;
    String directory,filename;

    static int      minSize = 1;            // in units?
    static int      maxSize = 999999;       // in units?
    static int      minTrackLength = 2;     // in frames
    static boolean  bSaveResultsFile = false;
    static float    maxVelocity = 150;      // in pixels per frame; this needs to be converted before filling the dialog
    static boolean  skipDialogue = false;
    static double   framesPerSecond = 614;

    public class particle {
        float   x;
        float   y;
        float   area;
        float   boundsX, boundsY, boundsWidth, boundsHeight;
        float   perimeter;
        float   deformationParameter; // This is (Width - Height)/(Width + Height), and shows roundness.

        // The Feret measures, also called "caliper" measures, indicate
        // the maximum length, minimum width, and angle of major
        // axis of a particle. The Feret deformation is the same
        // deformation parameter as above, but using the Feret measures.
        float   feretLength, feretWidth, feretAngle, feretDeformation;

        int     frameNumber;
        int     trackNr;
        float   velocity;
        boolean velocityIsValid;
        boolean inTrack = false;
        boolean flag = false;

        public void copy(particle source) {
            this.x                      = source.x;
            this.y                      = source.y;
            this.area                   = source.area;
            this.boundsX                = source.boundsX;
            this.boundsY                = source.boundsY;
            this.boundsWidth            = source.boundsWidth;
            this.boundsHeight           = source.boundsHeight;
            this.perimeter              = source.perimeter;
            this.frameNumber            = source.frameNumber;
            this.inTrack                = source.inTrack;
            this.flag                   = source.flag;
            this.velocity               = source.velocity;
            this.velocityIsValid        = source.velocityIsValid;
            this.deformationParameter   = source.deformationParameter;
            this.feretLength            = source.feretLength;
            this.feretWidth             = source.feretWidth;
            this.feretAngle             = source.feretAngle;
            this.feretDeformation       = source.feretDeformation;
        }

        public float distance (particle p) {
            return (float) Math.sqrt(sqr(this.x-p.x) + sqr(this.y-p.y));
        }
    }

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        cal = imp.getCalibration();
        if (IJ.versionLessThan("1.17y"))
            return DONE;
        else
            return DOES_8G+NO_CHANGES;
    }

   public static void setProperty (String arg1, String arg2) {
      if (arg1.equals("minSize"))
         minSize = Integer.parseInt(arg2);
      else if (arg1.equals("maxSize"))
         maxSize = Integer.parseInt(arg2);
      else if (arg1.equals("minTrackLength"))
         minTrackLength = Integer.parseInt(arg2);
      else if (arg1.equals("maxVelocity"))
         maxVelocity = Float.valueOf(arg2).floatValue();
      else if (arg1.equals("saveResultsFile"))
         bSaveResultsFile = Boolean.valueOf(arg2);
      else if (arg1.equals("skipDialogue"))
         skipDialogue = Boolean.valueOf(arg2);
   }

    public void run(ImageProcessor ip) {
      if (!skipDialogue) {
        String unit = cal.getUnit();
        GenericDialog gd = new GenericDialog("Droplet Tracker");
        gd.addNumericField("Minimum Object Size", minSize, 0, 6, unit);
        gd.addNumericField("Maximum Object Size", maxSize, 0, 6, unit);
        gd.addNumericField("Maximum Velocity", maxVelocity, 0, 6, unit + "/frame");
        gd.addNumericField("Minimum Track Length", minTrackLength, 0, 6, "frames");
        gd.addNumericField("Frame Rate", framesPerSecond, 8, 12, "FPS");
        gd.addCheckbox("Save Results File", bSaveResultsFile);
        gd.showDialog();
        if (gd.wasCanceled())
        return;

        minSize = (int)gd.getNextNumber();
        maxSize = (int)gd.getNextNumber();
        maxVelocity = (float)gd.getNextNumber();
        minTrackLength = (int)gd.getNextNumber();
        bSaveResultsFile = gd.getNextBoolean();
      }
      if (bSaveResultsFile) {
         SaveDialog sd=new  SaveDialog("Save Track Results","trackresults",".txt");
         directory=sd.getDirectory();
         filename=sd.getFileName();
      }
        track(imp, minSize, maxSize, maxVelocity, directory, filename);
    }
    

    public void track(ImagePlus imp, int minSize, int maxSize, float maxVelocity, String directory, String filename) {
        int nFrames = imp.getStackSize();
        if (nFrames<2) {
            IJ.showMessage("DropletTracker", "Stack required");
            return;
        }

        // Get real-world unit label (if it is available)
        String unit = cal.getUnit();

        ImageStack stack = imp.getStack();
        int options = 0; // set all PA options false

        // Find centroid, area, bounding box, and perimeter using the
        // particle analyzer
        // Also, find the Feret (caliper) measures.
        int measurements = CENTROID+AREA+RECT+PERIMETER+FERET;

        // Initialize results table
        // ParticleAnalyzer returns the bounding rectangle
        // under ROI_X, ROI_Y, ROI_WIDTH, ROI_HEIGHT
        ResultsTable rt = new ResultsTable();
        rt.reset();

        // create storage for particle positions
        // This is a list of lists: each frame index contains a list of 
        // all the particles found in that frame.
        List[] theParticles = new ArrayList[nFrames];

        // Count the number of particle motion tracks
        int trackCount=0;

        // record particle positions for each frame in an ArrayList
        for (int iFrame=1; iFrame<=nFrames; iFrame++) {
            theParticles[iFrame-1]=new ArrayList();
            rt.reset();

            // Run the particle analysis with the measurements we want (see above).
            ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize);
            pa.analyze(imp, stack.getProcessor(iFrame));

            float[] sxRes           = rt.getColumn(ResultsTable.X_CENTROID);              
            float[] syRes           = rt.getColumn(ResultsTable.Y_CENTROID);
            float[] areaRes         = rt.getColumn(ResultsTable.AREA);

            float[] boundsXRes      = rt.getColumn(ResultsTable.ROI_X);
            float[] boundsYRes      = rt.getColumn(ResultsTable.ROI_Y);
            float[] boundsWidthRes  = rt.getColumn(ResultsTable.ROI_WIDTH);
            float[] boundsHeightRes = rt.getColumn(ResultsTable.ROI_HEIGHT);

            float[] feretLengthRes  = rt.getColumn(ResultsTable.FERET);
            float[] feretWidthRes   = rt.getColumn(ResultsTable.MIN_FERET);
            float[] feretAngleRes   = rt.getColumn(ResultsTable.FERET_ANGLE);

            float[] perimeterRes    = rt.getColumn(ResultsTable.PERIMETER);

            if (sxRes==null)
                continue;

            for (int iPart = 0; iPart < sxRes.length; iPart++) {
                particle aParticle = new particle();

                aParticle.x=sxRes[iPart];
                aParticle.y=syRes[iPart];
                aParticle.area=areaRes[iPart];

                aParticle.boundsX = boundsXRes[iPart];
                aParticle.boundsY = boundsYRes[iPart];
                aParticle.boundsWidth = boundsWidthRes[iPart];
                aParticle.boundsHeight = boundsHeightRes[iPart];

                aParticle.feretLength = feretLengthRes[iPart];
                aParticle.feretWidth = feretWidthRes[iPart];
                aParticle.feretAngle = feretAngleRes[iPart];

                aParticle.perimeter = perimeterRes[iPart];
                aParticle.frameNumber = iFrame;
                theParticles[iFrame-1].add(aParticle);
            }
            IJ.showProgress((double)iFrame/nFrames);
        }

        // now assemble tracks out of the particle lists
        // Also record to which track a particle belongs in ArrayLists
        List theTracks = new ArrayList();

        for (int i=0; i<=(nFrames-1); i++) {
            IJ.showProgress((double)i/nFrames);
            for (ListIterator j=theParticles[i].listIterator();j.hasNext();) {
                particle aParticle=(particle) j.next();
                if (!aParticle.inTrack) {
                    // This must be the beginning of a new track
                    List aTrack = new ArrayList();
                    trackCount++;
                    aParticle.inTrack=true;
                    aParticle.trackNr=trackCount;
                    aTrack.add(aParticle);
                    // search in next frames for more particles to be added to track
                    boolean searchOn=true;
                    particle oldParticle=new particle();
                    particle tmpParticle=new particle();
                    oldParticle.copy(aParticle);
                    for (int iF=i+1; iF<=(nFrames-1);iF++) {
                        boolean foundOne=false;
                        particle newParticle=new particle();
                        for (ListIterator jF=theParticles[iF].listIterator();jF.hasNext() && searchOn;) { 
                            particle testParticle =(particle) jF.next();
                            float distance = testParticle.distance(oldParticle);
                            // record a particle when it is within the search radius, and when it had not yet been claimed by another track
                            if ( (distance < maxVelocity) && !testParticle.inTrack) {
                                // if we had not found a particle before, it is easy
                                if (!foundOne) {
                                    tmpParticle=testParticle;
                                    testParticle.inTrack=true;
                                    testParticle.trackNr=trackCount;
                                    newParticle.copy(testParticle);
                                    foundOne=true;
                                }
                                else {
                                    // if we had one before, we'll take this one if it is closer.  In any case, flag these particles
                                    testParticle.flag=true;
                                    if (distance < newParticle.distance(oldParticle)) {
                                        testParticle.inTrack=true;
                                        testParticle.trackNr=trackCount;
                                        newParticle.copy(testParticle);
                                        tmpParticle.inTrack=false;
                                        tmpParticle.trackNr=0;
                                        tmpParticle=testParticle;
                                    }
                                    else {
                                        newParticle.flag=true;
                                    }   
                                }
                            }
                            else if (distance < maxVelocity) {
                            // this particle is already in another track but could have been part of this one
                            // We have a number of choices here:
                            // 1. Sort out to which track this particle really belongs (but how?)
                            // 2. Stop this track
                            // 3. Stop this track, and also delete the remainder of the other one
                            // 4. Stop this track and flag this particle:
                                testParticle.flag=true;
                            }
                        }
                        if (foundOne)
                            aTrack.add(newParticle);
                        else
                            searchOn=false;
                        oldParticle.copy(newParticle);
                    }
                    theTracks.add(aTrack);
                }
            }
        }


        /* Now calculate velocities (and deformation parameters, while we're at it.) */

        for (ListIterator iT = theTracks.listIterator(); iT.hasNext();) {
            List bTrack=(ArrayList) iT.next();
            // filter by size; move on if too short
            if (bTrack.size() >= minTrackLength) {
                particle prevParticle = null;
                for (ListIterator k = bTrack.listIterator(); k.hasNext(); ) {
                    particle aParticle=(particle) k.next();

                    if (prevParticle != null) {
                        aParticle.velocity = aParticle.distance(prevParticle);
                        aParticle.velocityIsValid = true;
                    } else {
                        aParticle.velocityIsValid = false; // No previous particles, so no velocity available
                    }

                    aParticle.deformationParameter = (aParticle.boundsWidth - aParticle.boundsHeight) / (aParticle.boundsWidth + aParticle.boundsHeight);
                    aParticle.feretDeformation = (aParticle.feretLength - aParticle.feretWidth) / (aParticle.feretWidth + aParticle.feretLength);

                    prevParticle = aParticle;
                }
            }
        }

        // Create the column headings based on the number of tracks
        // with length greater than minTrackLength
        // since the number of tracks can be larger than can be accomodated by Excell, we deliver the tracks in chunks of maxColumns
        // As a side-effect, this makes the code quite complicated
        String strHeadings = "Particle Number" 
                                + "\tFrame Number" 
                                + "\tX Centroid (" + unit + ")" 
                                + "\tY Centroid (" + unit + ")" 
                                + "\tArea (" + unit + "^2)" 
                                + "\tPerimeter (" + unit + ")" 
                                + "\tBounds X (" + unit + ")" 
                                + "\tBounds Y (" + unit + ")" 
                                + "\tBounds Width (" + unit + ")" 
                                + "\tBounds Height (" + unit + ")" 
                                + "\tVelocity (" + unit + "/frame)" 
                                + "\tDeformation Parameter (W-H)/(W+H)" 
                                + "\tAmbiguous Movement?" 
                                + "\tFeret Length (" + unit + ")" 
                                + "\tFeret Width (" + unit + ")" 
                                + "\tFeret Angle (degrees)"
                                + "\tFeret Deformation (FL-FW)/(FL+FW)";

        String contents="";
        boolean writefile=false;
        if (filename != null) {
            File outputfile=new File (directory,filename);
            if (!outputfile.canWrite()) {
                try {
                    outputfile.createNewFile();
                }
                catch (IOException e) {
                    IJ.showMessage ("Error", "Could not create " + directory + filename);
                }
            }
            if (outputfile.canWrite())
                writefile=true;
            else
                IJ.showMessage ("Error", "Could not write to " + directory + filename);
        }

        // display the table of results
        IJ.setColumnHeadings(strHeadings);

        BufferedWriter writer = null;
        if (writefile) {
            try {
                File outputfile = new File (directory,filename);
                writer = new BufferedWriter (new FileWriter (outputfile));
                writer.write(strHeadings + "\n", 0, strHeadings.length() + 1);
            } catch (IOException e) {
                if (filename != null)
                    IJ.error ("An error occurred writing the file.\n\n" + e);
                else IJ.error ("The filename was null.\n\n" + e);
            }
        }

        // Iterate by tracks
        int trackNumber = 1;
        for (ListIterator iT = theTracks.listIterator(); iT.hasNext();) {

            List bTrack=(ArrayList) iT.next();

            // filter by size; move on if too short
            if (bTrack.size() >= minTrackLength) {
                String strLine = "";

                for (ListIterator k = bTrack.listIterator(); k.hasNext(); ) {
                    particle aParticle=(particle) k.next();
                    strLine += trackNumber + "\t" 
                                + aParticle.frameNumber + "\t" 
                                + aParticle.x + "\t" 
                                + aParticle.y + "\t" 
                                + aParticle.area + "\t" 
                                + aParticle.perimeter + "\t" 
                                + aParticle.boundsX + "\t" 
                                + aParticle.boundsY + "\t" 
                                + aParticle.boundsWidth + "\t" 
                                + aParticle.boundsHeight + "\t" 
                                + (aParticle.velocityIsValid ? aParticle.velocity : "") + "\t"
                                + aParticle.deformationParameter + "\t" 
                                + (aParticle.flag ? "true" : "") + "\t" 
                                + aParticle.feretLength + "\t" 
                                + aParticle.feretWidth + "\t" 
                                + aParticle.feretAngle + "\t"
                                + aParticle.feretDeformation + "\n";        
                }
                IJ.write(strLine);

                if (writefile){
                    try {
                        if (writer != null)
                            writer.write(strLine + "\n", 0, strLine.length() + 1);
                    } catch (IOException e) {
                        IJ.error ("An error occurred writing the file.\n\n" + e);
                    }
                }

                trackNumber++;
            }
        }
    }


    // Utility functions
    double sqr(double n) {return n*n;}
    
    int doOffset (int center, int maxSize, int displacement) {
        if ((center - displacement) < 2*displacement) {
            return (center + 4*displacement);
        }
        else {
            return (center - displacement);
        }
    }

    double s2d(String s) {
        Double d;
        try {d = new Double(s);}
        catch (NumberFormatException e) {d = null;}
        if (d!=null)
            return(d.doubleValue());
        else
            return(0.0);
    }

}


