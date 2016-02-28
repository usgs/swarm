package gov.usgs.volcanoes.swarm.picker.hypo71;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import gov.usgs.volcanoes.core.Hypo71.ControlCard;
import gov.usgs.volcanoes.core.Hypo71.CrustalModel;
import gov.usgs.volcanoes.core.Hypo71.Hypo71;
import gov.usgs.volcanoes.core.Hypo71.Hypo71.Results;
import gov.usgs.volcanoes.core.Hypo71.HypoArchiveOutput;
import gov.usgs.volcanoes.core.Hypo71.Hypocenter;
import gov.usgs.volcanoes.core.Hypo71.PhaseRecord;
import gov.usgs.volcanoes.core.Hypo71.Station;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.picker.Event;
import gov.usgs.volcanoes.swarm.picker.EventLocator;

public class Hypo71Adapter implements EventLocator {

  private static final Logger LOGGER = LoggerFactory.getLogger(Hypo71Adapter.class);
  private ControlCard controlCard;
  private Queue<PhaseRecord> phaseRecordsList;
  private Queue<Station> stationsList;
  private Queue<CrustalModel> crustalModelList;

  public Hypo71Adapter() {


    // defaults taken from hypo-test-case-1.properties
    controlCard =
        new ControlCard(0, 5.0, 50.0, 100.0, 1.78, 2, 1, 18, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0);

    crustalModelList = new LinkedList<CrustalModel>();
    crustalModelList.add(new CrustalModel(3.3, 0.0));
    crustalModelList.add(new CrustalModel(5.0, 1.0));
    crustalModelList.add(new CrustalModel(5.7, 4.0));
    crustalModelList.add(new CrustalModel(6.7, 15.0));
    crustalModelList.add(new CrustalModel(8.0, 25.0));

  }

  private void runHypo(Event event) throws IOException {

    Hypo71 hypoCalculator = new Hypo71();

    HypoArchiveOutput hy = new HypoArchiveOutput();
    hy.setControlCard(controlCard);
    for (PhaseRecord o : phaseRecordsList) {
      hy.getPhaseRecords().add(o);
    }

    for (Station o : stationsList) {
      hy.getStations().add(o);
    }

    for (CrustalModel o : crustalModelList) {
      hy.getCrustalModel().add(o);
    }

    try {
      hypoCalculator.calculateHypo71("", null, stationsList, crustalModelList, controlCard,
          phaseRecordsList, null);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Results result = hypoCalculator.getResults();

  }

  public void locate(Event event) {
    String error = null;
    try {
      generateHypoInputs(event);
    } catch (Exception ex) {
      ex.printStackTrace();
      error = "Cannot run hypo please ensure input file has correct data";
    }

    if (error == null) {
      try {
        Results hypoResult = null;
        hypoResult = runHypo();

        HypoResults hr = new HypoResults();
        hr.setAdjustmentsOutput(hypoResult.getAdjustmentIterations());
        hr.setDeletedStationsList(hypoResult.getDeletedStationsList());
        hr.setHypocenterOuput(hypoResult.getHypocenterOutput());
        hr.setMissingStationsList(hypoResult.getMissingStationsList());
        hr.setPrintOutput(hypoResult.getPrintOutput());
        hr.setPunchOutput(hypoResult.getPunchOutput());
        hr.setStationsResultList(hypoResult.getStationsResultList());
        hr.setStats(hypoResult.getStats());
        hr.setSummaryList(hypoResult.getSummaryList());

        if (hr.getHypocenterOuput() != null && hr.getHypocenterOuput().size() > 0) {

          List<Hypocenter> centers = hr.getHypocenterOuput();
          if (centers.size() > 0) {
            Swarm.getSelectedAttempt().setLatitude((double) centers.get(0).getLAT1());
            Swarm.getSelectedAttempt().setLongitude((double) centers.get(0).getLON1());
            Swarm.getSelectedAttempt().setDepth(centers.get(0).getZ());

          }
        }

        if (Swarm.getSelectedAttempt() != null) {
          Swarm.getSelectedAttempt().setHypoResultsAsBytes(hr);
          if (archiveCheck.isSelected()) {
            Swarm.getSelectedAttempt().setHypoInputArchiveFilePath(hypoTotalInputPath.getText());
          }
          Swarm.getSelectedAttempt().persist();
        }

        if (Swarm.getApplication().getHypoOuputMapFrame() == null) {
          Swarm.getApplication().setHypoOuputMapFrame(new HypoOuputMapFrame());
        }

        Swarm.getApplication().getHypoOuputMapFrame().setHy(hy);
        Swarm.getApplication().getHypoOuputMapFrame().setHypoOutput(hypoTotalInputPath.getText());
        Swarm.getApplication().getHypoOuputMapFrame().setResultText(hypoResult.getOutput());
        Swarm.getApplication().getHypoOuputMapFrame().setVisible(true);

      } catch (Exception e1) {
        e1.printStackTrace();
        JOptionPane.showMessageDialog(null,
            "Cannot run hypo, please verify hypo has all neccessary inputs: " + e1.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
      }

    } else {
      JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
    }

  }

  /**
   * Generates hypo inputs either from an archived file or from objects in memory
   */
  private void generateHypoInputs(Event event) {

    for (String channel : event.getChannels().keySet()) {
      String station = channel.split(" ")[0];
      if (stationsList.contains(channel)) {
        continue;
      } else {
        Metadata metadata = SwarmConfig.getInstance().getMetadata(channel);
        if (metadata != null) {
          stationsList.add(createStation(metadata));
        } else {
          LOGGER.error("Skipping {}, no metadata found.", channel);
        }
      }

      phaseRecordsList.clear();
      
      
      for (String st : stations) {
        List<Marker> pmarkers = Marker.listByStationAndTypeAndAttempt(
            Swarm.getSelectedAttempt().getId(), st, Marker.P_MARKER_LABEL);

        List<Marker> smarkers = Marker.listByStationAndTypeAndAttempt(
            Swarm.getSelectedAttempt().getId(), st, Marker.S_MARKER_LABEL);

        List<Marker> codaMarkers = Marker.listByStationAndTypeAndAttempt(
            Swarm.getSelectedAttempt().getId(), st, Marker.CODA_MARKER_LABEL);

        if (pmarkers.size() == 0 && smarkers.size() == 0) {
          System.out.println("WARNING: Marker (p or s) not found for station " + st + " using file "
              + hypoInputPath.getText() + ". Skipping this station.");
          continue;
        }

        if (codaMarkers.size() > 0 && pmarkers.size() == 0) {
          throw new Exception(
              "Must have a p marker with coda marker. No p marker found for coda marker for station: "
                  + st + " using file " + hypoInputPath.getText());
        }

        String prmk = null;
        float pkDate = 0f;
        int pHour = 0;
        int pMin = 0;
        float pSec = 0f;

        String smrk = null;
        float sSec = 0f;

        float timeDiffFromCodaToPInSec = 0;

        if (pmarkers.size() > 0) {
          Marker pMarker = pmarkers.get(0);
          prmk = pMarker.getIp_ep()
              + (pMarker.getUpDownUnknown().equals("Up") ? "U"
                  : (pMarker.getUpDownUnknown().equals("Down") ? "D" : ""))
              + (pMarker.getWeight() == null ? "0" : pMarker.getWeight().toString());
          Calendar c = Calendar.getInstance();
          c.setTimeInMillis(pMarker.getMarkerTime().getTime());
          int pmonth = c.get(Calendar.MONTH);
          int pyear = c.get(Calendar.YEAR);
          int pday = c.get(Calendar.DAY_OF_MONTH);
          pHour = c.get(Calendar.HOUR_OF_DAY);
          pMin = c.get(Calendar.MINUTE);
          pSec = c.get(Calendar.SECOND) + c.get(Calendar.MILLISECOND) / 1000f;

          pkDate = Float.parseFloat(Integer.toString(pyear - 1900)
              + (pmonth < 10 ? "0" + Integer.toString(pmonth) : Integer.toString(pmonth))
              + (pday < 10 ? "0" + Integer.toString(pday) : Integer.toString(pday)));

          if (codaMarkers.size() > 0) {
            Marker codaMarker = codaMarkers.get(0);
            long codaMarkerTime = codaMarker.getMarkerTime().getTime();
            long pMarkerTime = pMarker.getMarkerTime().getTime();
            long timeDiffFromCodaToP = Math.abs(codaMarkerTime - pMarkerTime);
            timeDiffFromCodaToPInSec = timeDiffFromCodaToP / 1000;
          }
        }

        if (smarkers.size() > 0) {
          Marker sMarker = smarkers.get(0);
          Calendar c = Calendar.getInstance();
          c.setTimeInMillis(sMarker.getMarkerTime().getTime());

          smrk = sMarker.getIs_es() + sMarker.getUpDownUnknown()
              + (sMarker.getWeight() == null ? "0" : sMarker.getWeight().toString());

          sSec = c.get(Calendar.SECOND) + c.get(Calendar.MILLISECOND) / 1000f;
        }

        phaseRecordsList
            .add(
                new PhaseRecord(st, prmk, // PRMK
                    (prmk != null && prmk.length() > 3) ? Float.parseFloat(prmk.substring(3, 4))
                        : 0,
                    (int) pkDate, pMin, pSec, sSec, smrk, // SMRK
                    0.0f, // WS
                    0.0f, // AMX TODO: calc this
                    0.0f, // PRX
                    0.0f, // CALC
                    0.0f, // CALX
                    "", // RMK
                    0.0f, // DT
                    timeDiffFromCodaToPInSec, // FMP
                    "", // "1.22",
                    'D', smrk != null ? "1" : "", "", // "SR01IPD0 691005120651.22",
                    ' ', "" // "IPD0"
        ));
      }
    }
    PhaseRecord lastRecordIndicator = new PhaseRecord();
    lastRecordIndicator.setMSTA("");
    phaseRecordsList.add(lastRecordIndicator);
  }

  private Station createStation(Metadata metadata) {
    Station station = new Station();

    String stationName = metadata.getChannel().split(" |$")[0];
    if (stationName.length() > 4) {
      stationName = stationName.substring(0, 4);
    }
    station.setNSTA(stationName);

    double lat = metadata.getLatitude();
    char ins = (lat < 0) ? 'S' : 'N';
    lat = Math.abs(lat);
    int latDegree = (int) Math.abs(lat);
    double latMin = (lat - latDegree) * 60;

    station.setLAT1(latDegree);
    station.setLAT2(latMin);
    station.setINS(ins);

    double lon = metadata.getLongitude();
    char iew = (lon < 0) ? 'W' : 'E';
    lon = Math.abs(lon);
    int lonDegree = (int) lon;
    double lonMin = (lon - lonDegree) * 60;

    station.setLON1(lonDegree);
    station.setLON2(lonMin);
    station.setIEW(iew);

    // TODO: add elevation to winston
    station.setIELV(0);

    // TODO: station delay needed?
    station.setDly(0);

    // TODO: station correction for FMEG needed?
    station.setFMGC(0);

    // TODO: station correction for XMEG needed?
    station.setXMGC(0);

    // TODO: System number?
    station.setKLAS(8);

    return station;
  }

}
