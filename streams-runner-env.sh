#!/bin/bash
# begin_generated_IBM_copyright_prolog                             
#                                                                  
# This is an automatically generated copyright prolog.             
# After initializing,  DO NOT MODIFY OR MOVE                       
# **************************************************************** 
# THIS SAMPLE CODE IS PROVIDED ON AN "AS IS" BASIS. IBM MAKES NO   
# REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, CONCERNING    
# USE OF THE SAMPLE CODE, OR THE COMPLETENESS OR ACCURACY OF THE   
# SAMPLE CODE. IBM DOES NOT WARRANT UNINTERRUPTED OR ERROR-FREE    
# OPERATION OF THIS SAMPLE CODE. IBM IS NOT RESPONSIBLE FOR THE    
# RESULTS OBTAINED FROM THE USE OF THE SAMPLE CODE OR ANY PORTION  
# OF THIS SAMPLE CODE.                                             
#                                                                  
# LIMITATION OF LIABILITY. IN NO EVENT WILL IBM BE LIABLE TO ANY   
# PARTY FOR ANY DIRECT, INDIRECT, SPECIAL OR OTHER CONSEQUENTIAL   
# DAMAGES FOR ANY USE OF THIS SAMPLE CODE, THE USE OF CODE FROM    
# THIS [ SAMPLE PACKAGE,] INCLUDING, WITHOUT LIMITATION, ANY LOST  
# PROFITS, BUSINESS INTERRUPTION, LOSS OF PROGRAMS OR OTHER DATA   
# ON YOUR INFORMATION HANDLING SYSTEM OR OTHERWISE.                
#                                                                  
# (C) Copyright IBM Corp. 2017, 2017  All Rights reserved.         
#                                                                  
# end_generated_IBM_copyright_prolog                               

# Set the environment variables relative to this script
export STREAMS_RUNNER_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
export STREAMS_BEAM_TOOLKIT=$STREAMS_RUNNER_HOME/com.ibm.streams.beam

# Inform users of new environment variables
echo "STREAMS_RUNNER_HOME has been set: $STREAMS_RUNNER_HOME"
echo "STREAMS_BEAM_TOOLKIT has been set: $STREAMS_BEAM_TOOLKIT"

# Check if STREAMS_INSTALL is set
if [[ -z $STREAMS_INSTALL ]]; then
  echo "No Streams installation is set. If you plan to compile and submit applications to a local Streams installation, setup the Streams environment."
fi
