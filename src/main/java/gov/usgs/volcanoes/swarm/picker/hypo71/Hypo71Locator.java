package gov.usgs.volcanoes.swarm.picker.hypo71;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.hypo71.ControlCard;
import gov.usgs.volcanoes.core.hypo71.CrustalModel;
import gov.usgs.volcanoes.core.hypo71.Hypo71;
import gov.usgs.volcanoes.core.hypo71.HypoArchiveOutput;
import gov.usgs.volcanoes.core.hypo71.Hypocenter;
import gov.usgs.volcanoes.core.hypo71.PhaseRecord;
import gov.usgs.volcanoes.core.hypo71.Station;
import gov.usgs.volcanoes.core.hypo71.Hypo71.Results;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.picker.Event;
import gov.usgs.volcanoes.swarm.picker.EventChannel;
import gov.usgs.volcanoes.swarm.picker.EventLocator;
import gov.usgs.volcanoes.swarm.picker.Hypocenters;
import gov.usgs.volcanoes.swarm.picker.Phase;

public class Hypo71Locator implements EventLocator {

  private static final Logger LOGGER = LoggerFactory.getLogger(Hypo71Locator.class);
  private ControlCard controlCard;
  private Queue<PhaseRecord> phaseRecordsList;
  private Queue<Station> stationsList;
  private Queue<CrustalModel> crustalModelList;
  private SimpleDateFormat jtimeFormat;

  public Hypo71Locator() {

    // defaults taken from hypo-test-case-1.properties
    controlCard =
        new ControlCard(0, 5.0, 50.0, 100.0, 1.78, 2, 1, 18, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0);

    crustalModelList = new LinkedList<CrustalModel>();
    crustalModelList.add(new CrustalModel(3.3, 0.0));
    crustalModelList.add(new CrustalModel(5.0, 1.0));
    crustalModelList.add(new CrustalModel(5.7, 4.0));
    crustalModelList.add(new CrustalModel(6.7, 15.0));
    crustalModelList.add(new CrustalModel(8.0, 25.0));

    jtimeFormat = new SimpleDateFormat("yyMMddHH");

    stationsList = new LinkedList<Station>();
    phaseRecordsList = new LinkedList<PhaseRecord>();
  }

  public void locate(Event event) throws IOException {
    generateHypoInputs(event);
    Results hypoResult = runHypo(event);
    LOGGER.debug(hypoResult.getPrintOutput());
    for (Hypocenter hypo : hypoResult.getHypocenterOutput()) {
      double lon = hypo.getLON1() + (hypo.getLON2() / 60);
      double lat = hypo.getLAT1() + (hypo.getLAT2() / 60);
      LOGGER.debug("Adding hypo: {}, {} ({} +{}, {} + {})", lon, lat, hypo.getLON1(),
          hypo.getLON2(), hypo.getLAT1(), hypo.getLAT2());
      Hypocenters.add(new gov.usgs.volcanoes.swarm.picker.Hypocenter(lat, lon));
    }
  }

  private Results runHypo(Event event) throws IOException {

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
    return result;

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
    }

    phaseRecordsList.clear();
    Map<String, EventChannel> eChans = event.getChannels();
    for (String chan : eChans.keySet()) {
      EventChannel eChan = eChans.get(chan);
      PhaseRecord phaseRecord = new PhaseRecord();
      phaseRecord.setMSTA(chan.split(" ")[0]);

      Phase pPhase = eChan.getPhase(Phase.PhaseType.P);
      float pSec = 0;
      if (pPhase != null) {
        String prmk = pPhase.onset + "P" + pPhase.firstMotion + pPhase.weight;
        phaseRecord.setPRMK(prmk);
        long time = pPhase.time;
        phaseRecord.setJTIME(Integer.parseInt(jtimeFormat.format(time)));
        int min = (int) ((time / 1000) / 60) % 60;
        phaseRecord.setJMIN(min);

        pSec = ((time / 1000) % 60) + ((time % 1000) / 1000f);
        phaseRecord.setP(pSec);
      }

      Phase sPhase = eChan.getPhase(Phase.PhaseType.S);
      if (sPhase != null) {
        String srmk = sPhase.onset + "S" + sPhase.firstMotion + sPhase.weight;
        phaseRecord.setSRMK(srmk);
        long time = sPhase.time;

        phaseRecord.setJTIME(Integer.parseInt(jtimeFormat.format(time)));

        float sSec = ((time / 1000) % 60) + ((time % 1000) / 1000f);
        if (sSec < pSec) {
          sSec += 60;
        }
        phaseRecord.setS(sSec);

        long cTime = eChan.getCodaTime();
        if (cTime > 0) {
          phaseRecord.setFMP(cTime = pPhase.time);
        }
        phaseRecord.setAS("1");
      } else {
        phaseRecord.setAS("");
      }

      phaseRecord.setSYM('D');
      phaseRecord.setRMK("");
      phaseRecordsList.add(phaseRecord);

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
