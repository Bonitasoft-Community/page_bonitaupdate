'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('bonitaupdateapp', ['ui.bootstrap','ngSanitize']);


/* Material : for the autocomplete
 * need 
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-animate.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-aria.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.5/angular-messages.min.js"></script>

  <!-- Angular Material Library -->
  <script src="https://ajax.googleapis.com/ajax/libs/angular_material/1.1.0/angular-material.min.js">
 */



// --------------------------------------------------------------------------
//
// Controler 
//
// --------------------------------------------------------------------------

// Bonita App Controller
appCommand.controller('BonitaUpdateController',
	function ( $http, $scope, $sce ) {



	// -----------------------------------------------------------------------------------------
	//  										Nav bar
	// -----------------------------------------------------------------------------------------

	this.navbaractiv='patches';
	
	this.getNavClass = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'ng-isolate-scope active';
		return 'ng-isolate-scope';
	}

	this.getNavStyle = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'border: 1px solid #c2c2c2;border-bottom-color: transparent;';
		return 'background-color:#cbcbcb';
	}

	

	// -----------------------------------------------------------------------------------------
	//  										Refresh
	// -----------------------------------------------------------------------------------------
	this.tango = {};
	this.listLocalPatches =[];
	this.listServerPatches= [];
	this.param = {
			"serverurl": "http://localhost:8080/bonita/API?page=custompage_bonitaupdate"	
	}
	this.inprogress=false;

	
	this.init = function()
	{
		var self=this;
		self.inprogress=true;
		self.listevents	="";
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
			
		var paramUrl = {  "param":this.param};

		var json = encodeURIComponent(angular.toJson(paramUrl, true));
	
		console.log("Call init");
		$http.get( '?page=custompage_bonitaupdate&action=init&paramjson='+json+'&t='+d.getTime() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					console.log("Receive answer jsonResult="+jsonResult);			
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					console.log("history",jsonResult);
					self.localPatches 			= jsonResult.localPatches;
					self.serverPatches 			= jsonResult.serverPatches;
					self.listevents				= jsonResult.listevents;
					self.tango					= jsonResult.tango;
					self.inprogress=false;
						
				})
				.error( function() {
					self.inprogress=false;
					});
				
	}
	this.init();
	
	/**
	 * refreshoperation is refresh or refreshserver
	 */
	this.refresh = function( refreshoperation )
	{
		var self=this;
		self.inprogress=true;
		self.listevents	="";
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
			
		var paramUrl = {  "tango":this.tango};
		
		var json = encodeURIComponent(angular.toJson(paramUrl, true));
	
	
		$http.get( '?page=custompage_bonitaupdate&action='+refreshoperation+'&paramjson='+json+'&t='+d.getTime() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					console.log("refresh",jsonResult);
					if (jsonResult.localPatches)
						self.localPatches 		= jsonResult.localPatches;
					
					if (jsonResult.serverPatches)
						self.serverPatches 			= jsonResult.serverPatches;

					self.listevents				= jsonResult.listevents;
					self.inprogress=false;
						
				})
				.error( function() {
					self.inprogress=false;
					});
				
	}
	
	
	this.getPatchStatus = function( status ) {
		if (status === "SERVER") 
			return "<div class='label label-default'>Available</div>";
		if (status ==="INSTALLED")
			return "<div class='label label-success'>Installed</div>";
		if (status ==="DOWNLOADED")
			return "<div class='label label-primary'>Downloaded</div>";
		if (status ==="DOWNLOADEDWITHSUCCESS")
			return "<div class='label label-info'>Downloaded</div>";
		if (status ==="DOWNLOADEDWITHERROR")
			return "<div class='label label-danger'>Error at Downloaded</div>";
		return status; 	
		
	}
	
	
	// -----------------------------------------------------------------------------------------
	//  										Download
	// -----------------------------------------------------------------------------------------
	var listPatchesDownload = [];
	this.downloadOnePatch = function( patch ) {
		this.listPatchesDownload = [ patch ];
		this.operationinstall('install');
	}
	this.downloadAllPatches = function( ) {
		var self=this;
		self.inprogress=true;
		self.listevents	=[];
		var paramUrl = { "param":this.param, "tango":this.tango};
		var json = encodeURIComponent(angular.toJson(paramUrl,true));
		var d = new Date();
		$http.get( '?page=custompage_bonitaupdate&action=downloadallpatches&paramjson='+json+'&t='+d.getTime() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				console.log("downloadResult=",jsonResult);
				self.serverPatches = jsonResult.serverPatches;
				
				
				self.inprogress=false;
				// refresh the local list now
				self.refresh('refresh');
					
			})
			.error( function() {
				self.inprogress=false;
				});
				
		
	
	}

	
	
	// -----------------------------------------------------------------------------------------
	//  										Install
	// -----------------------------------------------------------------------------------------
	var listPatchesSelected = [];
	
	
	
	this.installOnePatch = function( patch ) {
		this.listPatchesSelected = [ patch ];
		this.operationinstall('install');
	}
	
	this.operationinstall = function( operation ) {
		var self=this;
		
		self.inprogress=true;
		self.listevents	="";
		
		var listPatchAction = [];
		for (var i in this.listPatchesSelected) {
			listPatchAction.push( this.listPatchesSelected[ i ].name );
		}
		var paramUrl = { "patches":listPatchAction, "param":this.param};
		var json = encodeURIComponent(angular.toJson(paramUrl,true));
		var d = new Date();
		$http.get( '?page=custompage_bonitaupdate&action='+operation+'&paramjson='+json+'&t='+d.getTime() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					console.log("history",jsonResult);
					
					self.listevents				= jsonResult.listevents;
					if (jsonResult.listpatchesoperationstatus != null) {
						// search each patch in the list and update it
						// console.log("Start listpatchesoperationstatus listPatchSelected="+angular.toJson(self.listPatchesSelected));
						for (var i in self.listPatchesSelected) {
							// console.log("Search Local["+self.listPatchesSelected[ i ].name+" in ="+angular.toJson(jsonResult.listpatchesoperationstatus));
							for (var j in jsonResult.listpatchesoperationstatus) {
								
								// console.log("Comapre ["+self.listPatchesSelected[ i ].name+"] with ["+jsonResult.listpatchesoperationstatus[ j ].name+"]");
								if (self.listPatchesSelected[ i ].name === jsonResult.listpatchesoperationstatus[ j ].name) {
									// console.log("MATCH");
									self.listPatchesSelected[ i ].status			= jsonResult.listpatchesoperationstatus[ j ].status;
									self.listPatchesSelected[ i ].statusoperation 	= jsonResult.listpatchesoperationstatus[ j ].statusoperation;
									self.listPatchesSelected[ i ].statuslistevents 	= jsonResult.listpatchesoperationstatus[ j ].statuslistevents;
								}
							}
						}
					}
					
					self.inprogress=false;
						
				})
				.error( function() {
					self.inprogress=false;
					});
				
		
	}
	
	this.uninstallOnePatch= function(patch) {
		this.listPatchesSelected = [ patch ];
		this.operationinstall( 'uninstall');

	}
	// -----------------------------------------------------------------------------------------
	//  										Update
	// -----------------------------------------------------------------------------------------


	this.updateparameters = function() {
		console.log("update parameters");
		this.inprogress=true;

		var paramUrl = { "tango":this.tango};
		var json = encodeURIComponent(angular.toJson(paramUrl,true));
		var d = new Date();
		var self = this;
		self.listeventsupdate="";

		$http.get( '?page=custompage_bonitaupdate&action=updateparameters&paramjson='+json+'&t='+d.getTime() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					self.listeventsupdate = jsonResult.listevents;
					self.inprogress=false;
					console.log("updateParameters:",jsonResult);
				}
				);
	}
	// -----------------------------------------------------------------------------------------
	//  										Excel
	// -----------------------------------------------------------------------------------------

	this.exportData = function () 
	{  
		//Start*To Export SearchTable data in excel  
	// create XLS template with your field.  
		var mystyle = {         
        headers:true,        
			columns: [  
			{ columnid: 'name', title: 'Name'},
			{ columnid: 'version', title: 'Version'},
			{ columnid: 'state', title: 'State'},
			{ columnid: 'deployeddate', title: 'Deployed date'},
			],         
		};  
	
        //get current system date.         
        var date = new Date();  
        $scope.CurrentDateTime = $filter('date')(new Date().getTime(), 'MM/dd/yyyy HH:mm:ss');          
		var trackingJson = this.listprocesses
        //Create XLS format using alasql.js file.  
        alasql('SELECT * INTO XLS("Process_' + $scope.CurrentDateTime + '.xls",?) FROM ?', [mystyle, trackingJson]);  
    };
    


	
	<!-- Manage the event -->
	this.getHtml = function ( listevents, source ) {
		console.log("getHtml ["+source+"] html="+listevents);
		if (! listevents)
			return "";
		return $sce.trustAsHtml(  listevents );
	}

});



})();