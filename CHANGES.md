## Version 3.0.2 - February 3, 2020
  * Fix display of really small values in status area
  * Allow decimals for one bar range and clip value in helicorder settings (#309)
  * Allow decimals in min/max amp in wave settings for small values (#310)

## Version 3.0.1 - January 30, 2020
  * Fix plot tick labels for really small values (#306)
  * volcano-core dependency to 3.0.2
  * use new hypo71 1.0.0 and quakeml 1.0.0 libraries

## Version 3.0.0 - November 18, 2019
  * Support import of QuakeML without onset, polarity, and uncertainty (#292)
  * Improve loading of wave clipboard upon QuakeML import (#293)
  * Remove includeavailability option in FDSN query statement (#295)
  * Support QuakeML timestamp with six decimal places for seconds (#296)
  * Fix issue opening wave view from helicorder on Windows 10 against Winston 1.3.* (#299)
  * Require Java 8 or higher (previously supported Java 7)  
  * volcano-core dependency to 2.0.0
  * wwsclient dependency to 2.0.0

## Version 2.8.13 - October 2, 2019
  * Use Swarm config Vp/Vs for Hypo71 Control Card POS (#287)
  * Fix map display issue introduced in 2.8.12 (#288)

## Version 2.8.12 - September 23, 2019
  * Add option to hide stale channels in Data Chooser (#274)
  * Add color-blind friendly spectrum option for spectrogram (#275)
  * Indicate event classification color in tag menu (#279)
  * New default colors for default event classifications
  * Fix start time precision in Seisan file export (#280)
  * Fix wrong year in wave panel when date is Dec 31
  * Handle error when map view selected for station with no lat/lon in metadata

## Version 2.8.11 - June 3, 2019
  * Fix hypocenter display error for high magnitudes 
  * Winston client to version 1.3.7 (fix issue against Winston 1.3 on Windows)

## Version 2.8.10 - May 31, 2019 
  * Add legend for events on map (#192)
  * Option to plot event colors based on depth (#193)
  * Manual scale of y-axis for spectra view (#219)
  * Add option to apply helicorder view settings to all open helicorders (#237)
  * Read/write picks in SAC header on import/export (#249)
  * Set B & E fields in SAC header on export
  * Fixes for non-integer sample rate (#260)
  * Separate log power and min/max frequency option for spectra and spectrogram views (#268)
  
## Version 2.8.9 - March 14, 2019
  * Fix waveform export issue in clipboard (#254)
  * Fix QuakeML date parse precision problem (#255)

## Version 2.8.8 - February 19, 2019
  * Fix SeedLink stream break issues (#131)
  
## Version 2.8.7 - December 13, 2018
  * Bundle user manual into zip file (under docs)
  * Bundle Hypo71 manual into zip file (under docs)
  * Add tooltips for Hypo71 TEST settings.
  * Fix Pick menu Hide option
  * Fix memory error when retrieving old helicorder data from FDSN (#182)
  * Don't hide Event Dialog after plotting hypocenter (#234)
  * Handle null timezone when writing to config (#240) 
  * Fix gap issues when reading seed files (#241)

## Version 2.8.6 - August 8, 2018
  * Addition of map option to hide station icons (#124)
  * Add RSAM value of selected wave panel period to status bar (#103)
  * Fix issue with streaming failing on loss of data (#142)
  * Fix issue using WWS instrument time zone (#201)
  * Fix multiple event dialog showing up under Window menu (#216)
  * Fix problem parsing server response from CWB (#221)
  * Support height metadata for FDSN data source (#222)
  * Position real-time wave viewer and RSAM viewer when opening layout (#224)
  * Updated WWS Client to 1.3.5

## Version 2.8.5 - July 13, 2018
  * Support real-time wave viewer in layouts (#40)
  * Support RSAM viewer in layouts
  * Add audio alarm for RSAM (#44)
  * Add 6 and 8 week time span for RSAM viewer (#188)
  * Fix issue with zoom/scroll for cached data source (#197)
  * Add event classifier (#199)
  * Fix loading of groups from SwarmMetadata.config (#205)
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
