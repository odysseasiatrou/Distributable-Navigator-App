# Distributable Navigation App (Android and Java)
Application developed for academic purposes and implements a navigator for android.

This application is able to draw a colored line from your current position to a destination you wish by just tapping on the map
that is printed as the main layout . The back-end of the app is capable of handling multiple request from different users using the map 
reduce framework.


The back-end part was developed with eclipse and the front-end with AndroidStudio .


This readme file will soon be updated with more details.

Firstly before proceeding to the details of this project , let's first clearly mention what is a distributed environment for the 
newcomers .As i have already mentioned few lines above it was a project developed for a course during my academical studies . Thus , i 
have clear view of what many newcomers whould understand about this app . So let's define what is a distributed environment or more 
accurate what is a distributed system and then we will go on further analysis with what is the environment it is operating in .

Definition of Distributed System from techopedia :

A distributed system is a network that consists of autonomous computers that are connected using a distribution middleware. They help 
in sharing different resources and capabilities to provide users with a single and integrated coherent network.

Let's now proceed on what is this actually describing and let's talk in terms of what the environment will be . So :

Keyword No 1 :

It is a network . Network stands for a total number of minimum two PCs . Two Different PCs , autonomous as the reference of the 
definition is . But as we will mention later it is all about the two , as a minimum , different CPUs that are used .

Keyword No 2 :

It uses a distribution middleware . It refers to brokers and software that runs in all PCs to support the communication inside the 
network . One very popular middleware in the java world is CORBA . 

Keyword No 3 :

It provides a single and intergrated coherent network . This is the basis of the concept of distributed systems , a network of computers
that operate and produce results as it would be with one single pc . The distribution of operations improves the overall 
performance of an information system because of using the availability of more than one CPUs .

The Mapper, the Reducer, the Master and the anroid view is one logical part of the distributable app .

Master

The Master node has the following classes running on it :

EuclideanMetric :

The EuclideanMetric class is a custom way to calculate the distance between two places on the map using the L2 norm .

DirectionsObject :

The DirectionsObject is a class used to set the data needed every time for the main app to process in order to produce results .
These data are the coordinations a json that processes it as a string and a list of strings that contain the information existed in the
json file .

Comparator :

The Comparator class is used to compare the coordinations of a given position on the map depending on the lattitude and longtitude that
this has .

Master :

The Master class is used for the main operations of the app . Firstly , this class searches for cached results inside a simple 
ArrayList structure . Whether the result exists it will be returned or it will make a new request to all the MapWorker nodes and then 
it will send the results to the ReduceWorkers . It is developed for three MapWorkers and one ReduceWorker . In case the Map-Reduce
will not return any result it will make a new request to the MapWorker to get the result from GoogleAPI .

ReduceWorker

TO-DO

MapWorker

TO-DO

AndroidClient

TO-DO


.
