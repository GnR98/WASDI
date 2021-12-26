#!flask/bin/python
from flask import Flask
from flask import jsonify
from flask import request
import os
import wasdi
import json
import urllib.parse
import subprocess
import traceback
from distutils.dir_util import copy_tree
from os.path import sys

app = Flask(__name__)

@app.route('/run/<string:processId>', methods=['POST'])
def run(processId):
	
	print("[" + processId+ "] wasdiProcessorServer Started - ProcId = " + processId)
	
	# First of all be sure to be in the right path
	dir_path = os.path.dirname(os.path.realpath(__file__))	
	os.chdir(dir_path)
	print("wasdiProcessorServer: processor folder set")

	try:
		# Copy updated files from processor folder to the docker
		copy_tree("/wasdi", "/home/wasdi", update=1)
		print("[" + processId+ "] wasdiProcessorServer: processors files updated")
	except Exception as oE:
		print(f'[{processId}] wasdiProcessorServer: Unexpected error: {type(oE)}: {oE}\n{sys.exc_info()[0]}')
	except:
		print("[" + processId + "] error while copying\n" + traceback.format_exc())
	
	# Check if this is a help request
	if processId == '--help':
		
		print("[" + processId+ "] wasdiProcessorServer Help Request: calling processor Help")
		
		sHelp = ""
		
		#Try to get help from the processor
		try:
			sHelpFileName = ""
			
			if os.path.isfile("readme.md"):
				sHelpFileName = "readme.md"
			elif os.path.isfile("README.md"):
				sHelpFileName = "README.md"
			elif os.path.isfile("README.MD"):
				sHelpFileName = "README.MD"
			elif os.path.isfile("readme.MD"):
				sHelpFileName = "readme.MD"
			elif os.path.isfile("help.md"):
				sHelpFileName = "help.md"
			elif os.path.isfile("help.MD"):
				sHelpFileName = "help.MD"
			elif os.path.isfile("HELP.MD"):
				sHelpFileName = "HELP.MD"
			if os.path.isfile("readme.txt"):
				sHelpFileName = "readme.txt"
			elif os.path.isfile("README.txt"):
				sHelpFileName = "README.txt"
			elif os.path.isfile("README.TXT"):
				sHelpFileName = "README.TXT"
			elif os.path.isfile("readme.TXT"):
				sHelpFileName = "readme.TXT"
			elif os.path.isfile("help.txt"):
				sHelpFileName = "help.txt"
			elif os.path.isfile("help.TXT"):
				sHelpFileName = "help.TXT"
			elif os.path.isfile("HELP.TXT"):
				sHelpFileName = "HELP.TXT"
						
			if not sHelpFileName == "":
				with open(sHelpFileName, 'r') as oHelpFile:
					sHelp = oHelpFile.read()
								
		except AttributeError as oE:
			print(f'[{processId}] wasdiProcessorServer Help not available ({type(oE)}: {oE})')
			sHelp = "No help available. Just try."
		except Exception as oE:
			print(f'[{processId}] wasdiProcessorServer Help not available ({type(oE)}: {oE})')
			sHelp = "No help available. Just try."
		except:
			# todo catch BaseException or something
			print("[" + processId + "] error while looking for documentation\n" + traceback.format_exc())

		# Return the available help			
		return jsonify({'help': sHelp})
	
	# Check if this is a lib update request
	if processId == '--wasdiupdate':
		#Try to update the lib
		try:
			print("[" + processId+ "] Calling pip upgrade")
			oProcess = subprocess.Popen(["pip", "install", "--upgrade", "wasdi"])
			print("[" + processId+ "] pip upgrade done")
		except Exception as oEx:
			print(f'[{processId}] wasdi.executeProcessor EXCEPTION: {type(oEx)}: {oEx}')
			print(repr(oEx))
			print(traceback.format_exc())
		except:
			# todo catch BaseException or something
			print("[" + processId+ "] wasdi.executeProcessor generic EXCEPTION while updating\n" + traceback.format_exc())
		
		# Return the result of the update
		return jsonify({'update': '1'})	
	
	# Check if this is a lib update request
	if processId.startswith('--kill'):
		#Try to update the lib
		try:
			asKillParts = processId.split("_")
			
			#TODO safety check or something
			print("[" + processId+ "] Killing subprocess")
			oProcess = subprocess.Popen(["kill", "-9", asKillParts[1]])
			print("[" + processId+ "] Subprocess killed")
		except Exception as oEx:
			print(f'[{processId}] wasdi.executeProcessor EXCEPTION ({type(oEx)}: {oEx})')
			print("[" + processId+ "] " + repr(oEx))
			print("[" + processId+ "] " + traceback.format_exc())
		except:
			# todo catch BaseException or something
			print("[" + processId + "] wasdi.executeProcessor generic EXCEPTION while killing\n" + traceback.format_exc())
		
		# Return the result of the update
		return jsonify({'kill': '1'})		
	
	print("[" + processId+ "] wasdiProcessorServer run request")
	
	# This is not a help request but a run request.
	
	# Copy request json in the parameters array
	parameters = request.json
			
	#Add, if present, embedded params
	try:
		if (os.path.isfile('params.json')):
			with open('params.json') as oFile:
				oEmbeddedParams = json.load(oFile)
				
				for sKey in oEmbeddedParams:
					if (not (sKey in parameters)):
						parameters[sKey] = oEmbeddedParams[sKey]
			print("[" + processId+ "] wasdiProcessorServer Added Embedded Params") 
		else:			
			print("[" + processId+ "] wasdiProcessorServer no Embedded Params available")
			

	except Exception as oE:
		print(f'[{processId}] wasdiProcessorServer Error in reading params.json {type(oE)}: {oE}')
	except:
		# todo catch BaseException or something
		print("[" + processId+ "] wasdiProcessorServer Error in reading params.json\n"  + traceback.format_exc())
	
	#Force User Session Workspace and myProcId from the Query Params
	if (request.args.get('user') is not None):
		parameters['user'] = request.args.get('user')
	else:
		print("[" + processId+ "] USER arg not available")

	if (request.args.get('sessionid') is not None):
		parameters['sessionid'] = request.args.get('sessionid')
	else:
		print("[" + processId+ "] SESSION arg not available")
		
	if (request.args.get('workspaceid') is not None):
		parameters['workspaceid'] = request.args.get('workspaceid')
	else:
		print("[" + processId+ "] WORKSPACE arg not available")
	
	#Try to get the user
	try:
		sUser = parameters['user']
		wasdi.setUser(sUser)
		print("[" + processId+ "] wasdiProcessorServer User available in params. Got " + sUser)
	except Exception as oE:
		print(f'[{processId}]  wasdiProcessorServer user not available in parameters. ({type(oE)}: {oE})')
	except:
		#todo catch BaseException or something
		print("[" + processId+ "] wasdiProcessorServer user not available in parameters.\n"+ traceback.format_exc())
		
	#Try to get the password
	try:
		sPassword = parameters['password']
		wasdi.setPassword(sPassword)
		print("[" + processId+ "] wasdiProcessorServer Pw available in params")
	except Exception as oE:
		print(f'[{processId}] wasdiProcessorServer password not available in parameters: {type(oE)}: {oE}')
	except:
		#todo catch BaseException or something
		print("[" + processId+ "] wasdiProcessorServer password not available in parameters.\n"+traceback.format_exc())
		
	#Try to get the session id
	try:
		sSessionId = parameters['sessionid']
		wasdi.setSessionId(sSessionId)
		print("[" + processId+ "] wasdiProcessorServer Session available in params " + sSessionId)
	except Exception as oE:
		print(f'[{processId}] wasdiProcessorServer Session not available in parameters: {type(oE)}: {oE}')
	except:
		# todo catch BaseException or something
		print("[" + processId+ "] wasdiProcessorServer Session not available in parameters.\n"+traceback.format_exc())
	
	#Try to set the proc id
	try:
		wasdi.setProcId(processId)
		print("[" + processId+ "] wasdiProcessorServer set Proc Id " + processId)
	except Exception as oE:
		print(f'[{processId}]  wasdiProcessorServer Proc Id not available: {type(oE)}: {oE}')
	except:
		print("[" + processId+ "] wasdiProcessorServer Proc Id not available")
		
	#Try to get the workspace id
	try:
		sWorkspaceId = parameters['workspaceid']
		wasdi.openWorkspaceById(sWorkspaceId)
		print("[" + processId+ "] wasdiProcessorServer Workspace Id available in params " + sWorkspaceId)
	except Exception as oE:
		print(f'[{processId}] wasdiProcessorServer Workspace Id not available in parameters: {type(oE)}: {oE}')
	except:
		# todo catch BaseException or something
		print("[" + processId+ "] wasdiProcessorServer Workspace Id not available in parameters.")


	#Try to get the base url
	try:
		sBaseUrl = parameters['baseurl']
		wasdi.setBaseUrl(sBaseUrl)
		print("[" + processId+ "] wasdiProcessorServer Base Url in params " + sBaseUrl)
	except Exception as oE:
		print(f'[{processId}] wasdiProcessorServer Using default base url: {type(oE)}: {oE}')
	except:
		# todo catch BaseException or something
		print("[" + processId+ "] wasdiProcessorServer Using default base url")

	
	#Init Wasdi
	print("[" + processId+ "] wasdiProcessorServer: init waspy lib")
	wasdi.setIsOnServer(True)
	wasdi.setDownloadActive(False)
	
	if wasdi.init() == False:
		print("[" + processId+ "] wasdiProcessorServer: init FAILED")
		return jsonify({'processId': 'ERROR', 'processorEngineVersion':'2'})
	
	#Run the processor
	try:
		wasdi.wasdiLog("wasdiProcessorServer RUN " + processId)
				
		#Run the processor in a separate process
		sStringParams = json.dumps(parameters)
		
		sEncodeParameters = urllib.parse.quote(sStringParams, safe='')
		oProcess = subprocess.Popen(["python", "wasdiProcessorExecutor.py", sEncodeParameters, processId])
		
		wasdi.wasdiLog("wasdiProcessorServer Process Started with local pid "  + str(oProcess.pid))
		#Update the server with the subprocess pid
		wasdi.setSubPid(processId, int(oProcess.pid))
		
	except Exception as oEx:
		wasdi.wasdiLog(f"wasdiProcessorServer EXCEPTION: {type(oEx)}: {oEx}")
		wasdi.wasdiLog(repr(oEx))
		wasdi.updateProcessStatus(processId, "ERROR", 100)
	
	return jsonify({'processId': processId, 'processorEngineVersion':'2'})

@app.route('/hello', methods=['GET'])
def hello():
	print("wasdiProcessoServer Hello request")
	return jsonify({'hello': 'hello waspi'})

if __name__ == '__main__':
	app.run(host='0.0.0.0', debug=False, use_reloader=False)