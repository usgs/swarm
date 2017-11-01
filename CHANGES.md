## Version 2.8.1
  * Upgraded seedlink library
  * Corrected reliability of wave display of gappy data
  * Fix loading of crustal model file
  * Fix hypo71 bug when checking for hemisphere
  * Fix FDSN WS opening on Swarm launch
  * RSAM ratio feature
  * Removal of RSAM filtering option
  
## Version 2.8.0
  * Hypo71 support
  * RSAM filtering option
  * Fix NullPointerException bug on Swarm config load

## Version 2.7.4
  * Fix clipboard image issue
  * Fix filter (f) and rescale (r) hot keys

## Version 2.7.3
  * Fix pick time zone on export to QuakeML
  * Add 'Clear All Picks' to pick menu bar
  * Stream line pick menu (right-click)
  * Add ability to use P pick for coda calculations
  * Add optional comment field for event export
  * Add clipboard button to event viewer
  * Fix sort button on event viewer
  * Update basemap URLs to https
  * Center map on imported events

## Version 2.7.2
  * Read WIN files
  * Add option to turn off S-P plot for a station
  * Remove $ from channel name display in pick wave panels
  * Fix problem with file type option when opening files from clipboard

## Version 2.7.1
  * Pick Mode enhancements:
  	- Add ability to select pick uncertainty
	- Plot S-P in map 
	- QuakeML import/export
	- Add key stroke shortcut
  * Fix bug in map line color selection

## Version 2.7.0
  * Add pick mode to Wave Clipboard
  * Add ability to import QuakeML files
  * Fix error saving config on exit
  * Fix error reading/writing channels in Seisan files
  * Fix incorrect time in status bar
  * Add general/debug info to particle motion plot for user 

## Version 2.6.3
  * Enable refresh of data source
  * Addition of initial capabilities for particle motion plot
  * Allow status bar to expand vertically to display entire text

## Version 2.6.2
  * Write multiplexed Seisan files
  * Use UTC for clipboard "go to time" button
  * Use 6 digits for the fraction portion of frequency status lines
  * Correct station encoding in SAC headers
  
## Version 2.6.1
  * write SAC files with correct headers
  * Use HTTPS for earthquake summary files
  * Fix error saving Layouts
  * code cleanup
  
## Version 2.6.0
  * Add option to plot hypocenters from NEIC summary files on map
  * Add event inspector dialog
  * Expand maximum time span of inset wave window
  * Quote command line args before passing them to java
  * Fix wave close buttons
  * Fix kisok enter/exit keys
  
## Version 2.5.2
  * Remove obsoleted DefaultMetadata Class.
  
## Version 2.5.1
  * Add missing libraries
  * Correct overflow when working with long waves
  
## Version 2.5
  * SCM migrated from in-house subversion to git hosted at GitHub
  * Build migrated from Ant to Maven
  * IRISWS client upgraded to support FDSNWS. (Thanks to Ivan Henson!)