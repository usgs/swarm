## Version 3.2.0 - TBD
  * Fix issue with reading WIN file for channels with 0.5 data size
  * Fix issues adjusting scale using left and right brackets in wave views
  * Fix helicorder update issue when accessing FDSN WS
  * Add option to always tile helicorders horizontally (for when there are 4)
  * Add feature to sort waves by pick times in clipboard

## Version 3.1.0 - February 18, 2020
  * Allow decimals for one bar range and clip value in helicorder settings 
  * Allow decimals in min/max amp in wave settings for small values 
  * Fix plot tick labels for really small values 
  * Fix display of really small values in status area
  * volcano-core dependency to 3.0.2
  * use new hypo71 1.0.0 and quakeml 1.0.0 libraries
  * Migration of source code from github.com/usgs to code.usgs.gov

## Version 3.0.0 - November 18, 2019
  * Support import of QuakeML without onset, polarity, and uncertainty 
  * Improve loading of wave clipboard upon QuakeML import 
  * Remove includeavailability option in FDSN query statement 
  * Support QuakeML timestamp with six decimal places for seconds 
  * Fix issue opening wave view from helicorder on Windows 10 against Winston 1.3.x 
  * Require Java 8 or higher (previously supported Java 7)  
  * volcano-core dependency to 2.0.0
  * wwsclient dependency to 2.0.0

## Version 2.8.13 - October 2, 2019
  * Use Swarm config Vp/Vs for Hypo71 Control Card POS 
  * Fix map display issue introduced in 2.8.12 

## Version 2.8.12 - September 23, 2019
  * Add option to hide stale channels in Data Chooser 
  * Add color-blind friendly spectrum option for spectrogram 
  * Indicate event classification color in tag menu 
  * New default colors for default event classifications
  * Fix start time precision in Seisan file export 
  * Fix wrong year in wave panel when date is Dec 31
  * Handle error when map view selected for station with no lat/lon in metadata

## Version 2.8.11 - June 3, 2019
  * Fix hypocenter display error for high magnitudes 
  * Winston client to version 1.3.7 (fix issue against Winston 1.3 on Windows)

## Version 2.8.10 - May 31, 2019 
  * Add legend for events on map 
  * Option to plot event colors based on depth 
  * Manual scale of y-axis for spectra view 
  * Add option to apply helicorder view settings to all open helicorders 
  * Read/write picks in SAC header on import/export 
  * Set B & E fields in SAC header on export
  * Fixes for non-integer sample rate 
  * Separate log power and min/max frequency option for spectra and spectrogram views
  
## Version 2.8.9 - March 14, 2019
  * Fix waveform export issue in clipboard 
  * Fix QuakeML date parse precision problem 

## Version 2.8.8 - February 19, 2019
  * Fix SeedLink stream break issues 
  
## Version 2.8.7 - December 13, 2018
  * Bundle user manual into zip file (under docs)
  * Bundle Hypo71 manual into zip file (under docs)
  * Add tooltips for Hypo71 TEST settings.
  * Fix Pick menu Hide option
  * Fix memory error when retrieving old helicorder data from FDSN
  * Don't hide Event Dialog after plotting hypocenter 
  * Handle null timezone when writing to config 
  * Fix gap issues when reading seed files 

## Version 2.8.6 - August 8, 2018
  * Addition of map option to hide station icons 
  * Add RSAM value of selected wave panel period to status bar 
  * Fix issue with streaming failing on loss of data 
  * Fix issue using WWS instrument time zone 
  * Fix multiple event dialog showing up under Window menu 
  * Fix problem parsing server response from CWB 
  * Support height metadata for FDSN data source 
  * Position real-time wave viewer and RSAM viewer when opening layout
  * Updated WWS Client to 1.3.5

## Version 2.8.5 - July 13, 2018
  * Support real-time wave viewer in layouts 
  * Support RSAM viewer in layouts
  * Add audio alarm for RSAM 
  * Add 6 and 8 week time span for RSAM viewer 
  * Fix issue with zoom/scroll for cached data source 
  * Add event classifier 
  * Fix loading of groups from SwarmMetadata.config 
  * Fix tooltip for Particle Motion shortcut
  * Fix URL for Imagery Topo map
  * Fix earth button on map to show full extent

## Version 2.8.4 - April 6, 2018
  * Allow input of Vp/Vs ratio under File->Options
  * Add option to save configuration file through File menu
  * Correct excessive CPU consumption when using seedlink
 
## Version 2.8.3 - February 2, 2018
  * Update seisFile library to 1.8.0
  * Read blockette100 sample rate in SEED file if it exists
  * Add xmlns:q and xmlns attributes to exported QuakeML files
  * Allow 2 decimal places for Butterworth Filter corners
  * Support Hypo71 KSING option
  * Fix printing of 5 char station names in Hypo71 output
  * Display start date on clipboard waveforms

## Version 2.8.2 - November 9, 2017
  * Save clipboard to layout
  * RSAM plot screen capture
  * Use S-picks in location algorithm
  * Fix Hypo71.config read 

## Version 2.8.1 - November 6, 2017
  * Upgraded seedlink library
  * Corrected reliability of wave display of gappy data
  * Fix loading of crustal model file
  * Fix hypo71 bug when checking for hemisphere
  * Fix FDSN WS opening on Swarm launch
  * RSAM ratio feature
  * Removal of RSAM filtering option
  
## Version 2.8.0 - August 16, 2017
  * Hypo71 support
  * RSAM filtering option
  * Fix NullPointerException bug on Swarm config load

## Version 2.7.4 - August 4, 2017
  * Fix clipboard image issue
  * Fix filter (f) and rescale (r) hot keys

## Version 2.7.3 - July 7, 2017
  * Fix pick time zone on export to QuakeML
  * Add 'Clear All Picks' to pick menu bar
  * Stream line pick menu (right-click)
  * Add ability to use P pick for coda calculations
  * Add optional comment field for event export
  * Add clipboard button to event viewer
  * Fix sort button on event viewer
  * Update basemap URLs to https
  * Center map on imported events

## Version 2.7.2 - June 6, 2017
  * Read WIN files
  * Add option to turn off S-P plot for a station
  * Remove $ from channel name display in pick wave panels
  * Fix problem with file type option when opening files from clipboard

## Version 2.7.1 - May 22, 2017
  * Pick Mode enhancements:
  	- Add ability to select pick uncertainty
	- Plot S-P in map 
	- QuakeML import/export
	- Add key stroke shortcut
  * Fix bug in map line color selection

## Version 2.7.0 - May 10, 2017
  * Add pick mode to Wave Clipboard
  * Add ability to import QuakeML files
  * Fix error saving config on exit
  * Fix error reading/writing channels in Seisan files
  * Fix incorrect time in status bar
  * Add general/debug info to particle motion plot for user 

## Version 2.6.3 - April 24, 2017
  * Enable refresh of data source
  * Addition of initial capabilities for particle motion plot
  * Allow status bar to expand vertically to display entire text

## Version 2.6.2 - March 3, 2017
  * Write multiplexed Seisan files
  * Use UTC for clipboard "go to time" button
  * Use 6 digits for the fraction portion of frequency status lines
  * Correct station encoding in SAC headers
  
## Version 2.6.1 - February 21, 2017
  * write SAC files with correct headers
  * Use HTTPS for earthquake summary files
  * Fix error saving Layouts
  * code cleanup
  
## Version 2.6.0 - February 6, 2017
  * Add option to plot hypocenters from NEIC summary files on map
  * Add event inspector dialog
  * Expand maximum time span of inset wave window
  * Quote command line args before passing them to java
  * Fix wave close buttons
  * Fix kisok enter/exit keys
  
## Version 2.5.9 - March 11, 2016
  * Bug fixes
  
## Version 2.5.8 - February 26, 2016
  * Bug fixes
  
## Version 2.5.2
  * Remove obsoleted DefaultMetadata Class.
  
## Version 2.5.1 - October 14, 2015
  * Add missing libraries
  * Correct overflow when working with long waves
  
## Version 2.5 - October 14, 2015
  * SCM migrated from in-house subversion to git hosted at GitHub
  * Build migrated from Ant to Maven
  * IRISWS client upgraded to support FDSNWS. (Thanks to Ivan Henson!)
