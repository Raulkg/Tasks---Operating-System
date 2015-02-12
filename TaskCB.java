/**********************************************************************************
	CIS 657.M001 Principles of Operating Systems Lab Assignment
	Project 1: Tasks

	Group Members:
		Kranthi Kumar Polisetty
		Rahul Kumar Gaddam
		Rao Yan
		Sujit Poudel
		
	Project Goal: The goal of this project is to implement Tasks object for
	simulated OSP 2 Operating systems, given all other complete modules in OSP.
***********************************************************************************/

/***************************************************************************************************************************
	We utilize ArrayList as the data structure to handle the table of Files/Threads/Ports in this project. Arraylist 
	is capable of modifying elements in the list dynamicly without interferring a simontaneous iteration, this approuch
	satisfied the assignment criteria and carries simplistic yet intuitive implementation.
	do_Create() Creates new TaskCB object by invoking the constructor, with tables of its resources attached to the object.
	pagetable, creation time and status have been set up as well. Due to absence of explicit instruction in the 
	assignment description, task priority has been set to an arbitrary number 0. 
	Should there be any Error or Warning occured during the execution of OSP2, relavant messages would be output to console. 
	What's SwapPathFile? Is it an OpenFile Instance of the swapfile? (Line 51)
****************************************************************************************************************************/

package osp.Tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Hardware.*;

/*@OSPProject Tasks*/
public class TaskCB extends IflTaskCB
{
	private List<OpenFile> Files; //The list of files that the task is utilizing
	private List<ThreadCB> Threads; //The threads this task is using
	private List<PortCB> Ports; //The ports this task is using
	
	private PageTable PT; //We need to track the PageTable object that this task is using 
	
	static String Warning; //The warning static variable to handle warnings
	static String Error; //The error static variable to handle error messages
	
	private ThreadCB TCB;
	private OpenFile SwapPathFile;

	private String SwapPath;
	private FileSys SwapFile; //Swap File

	/**
		The default constructor
	*/
	public TaskCB()
	{
			super(); //Always needed
	}

	/**
		If needed to initialize any static variable, they would go here. This is called once
	*/
	public static void init()	{}

	/**
		This is where we are going to create an actual task. We need an instance of TaskCB object,
		need to set the page table parameters and swap file.
		If there was some error with swap file creation, we should be returning null or else, return
		the TaskCB object
		@return task or null
		@OSPProject Tasks
	*/
	static public TaskCB do_create()
	{
		TaskCB NewTask = new TaskCB(); //Instance of TaskCB created. We will return this in case of successful completion of this Task creation
		
		//Need to initialize our list for Threads, Ports, and Files.
		NewTask.Threads = new ArrayList<>();
		NewTask.Ports = new ArrayList<>();
		NewTask.Files = new ArrayList<>();
		
		//Setup page table
		PageTable PT = new PageTable(NewTask);
		NewTask.setPageTable(PT);
		NewTask.PT = PT;

		NewTask.setCreationTime(HClock.get()); //Set the task creation time to be current time given by HTClock
		NewTask.setStatus(TaskLive); //Need to set the task to be live using TaskLive
		NewTask.setPriority(0);//The priority number value has not been decided, so we are simply setting an arbitary 0 at this point.
		String SwapPath = SwapDeviceMountPoint + Integer.toString(NewTask.getID()); //Need to get a unique Swap file path for this task

		
		NewTask.SwapFile.create(SwapPath, (int)Math.pow(2.0D, MMU.getVirtualAddressBits())); //To create a swap file, we need to send a path, and size
		OpenFile SwapPathFile = OpenFile.open(SwapPath, NewTask); //Finally open the swap file

		NewTask.setSwapFile(SwapPathFile);//And set the swap file

		ThreadCB.create(NewTask);//Run the new task in the thread we have defined for this task
		
		
		//**** add test for SwapFileSuccess == FAILURE or SwapPathFile is assigned nothing (null)- if so, according to p.48, dispatch a new thread, and return null
		// shouldn't be necessary to specifically call dispatch() for the thread, according to bottom of p.48 - footnote 1
		if (SwapPathFile == null)
		{
			Error = "There was some error in Swap file allocation";
			NewTask.Threads.get(0).dispatch();	//Now dispatch the thread at vector position 0
			return null;
		}
		return NewTask; //Finally, return the task
	}

	/**
		Kills the specified task and all of its threads.
		@OSPProject Tasks
	*/
	public void do_kill()
	{
		//Iterate through the Threads of this task and kill'em all
		int Size = this.Threads.size();
		while (Size > 0) 
		{
			this.Threads.get(Size-1).kill();
			Size--;			
		}

		//Destruction: destroy all ports created by this task
		Size = this.Ports.size();
		while (Size > 0) 
		{
			this.Ports.get(Size-1).destroy();
			Size--;			
		}

		//Close all the files (now junk for this task)
		Size = this.Files.size();
		while (Size > 0) 
		{
			this.Files.get(Size-1).close();
			Size--;
		}
		
		//To properly close the task, we need to delete the unique swap file we created for this task, set the status to TaskTerm, deallocate the pagetable memory
		this.SwapFile.delete(SwapDeviceMountPoint + String.valueOf(this.getID()));
		this.setStatus(TaskTerm);
		this.PT.deallocateMemory();
		//this.getPageTable().deallocateMemory(); //This is also fine to do
	}

	//Simple method to return the number of threads, which basically is the size of Threads list
	public int do_getThreadCount()
	{
		return this.Threads.size();
	}

	/**
		Adds the specified thread to this task. 
		@param thread
		@return FAILURE, if the number of threads exceeds MaxThreadsPerTask;
		SUCCESS otherwise.
		@OSPProject Tasks
	*/
	public int do_addThread(ThreadCB thread)
	{
		// your code goes here
		if (Threads.size() < ThreadCB.MaxThreadsPerTask)
		{
			if (Threads.add(thread))
			{
				return SUCCESS;
			}
			else
			{
				Error = "There was some error adding the thread.";
				atError();
				return FAILURE;
			}
		}
		else Error = "Max Threads Per Task limit exceeded!";
		return FAILURE;
	}

	/**
		Removes the specified thread from this task.
		@param thread
		@return FAILURE or SUCCESS 
		@OSPProject Tasks
	*/
	public int do_removeThread(ThreadCB thread)
	{
		// your code goes here
		if (this.Threads.contains(thread))
		{
			if (this.Threads.remove(thread))
			{
				return SUCCESS;
			}
			else
			{
				Error = "There was some error removing thread from the list";
				atError();
				return FAILURE;
			}
		}
		else
		{
			Warning = "The thread does not exist on the list";
			atWarning();
		}
		return FAILURE;
	}

	/**
		Return number of ports currently owned by this task.
		@return Total Ports
		@OSPProject Tasks
	**/
	public int do_getPortCount()
	{
			return this.Ports.size();
	}

	/**
		Add the port to the list of ports owned by this task.
		@param newPort	
		@return SUCCESS or FAILURE
		@OSPProject Tasks 
	*/ 
	public int do_addPort(PortCB newPort)
	{
			if (this.Ports.size() <= PortCB.MaxPortsPerTask)
			{
					if (this.Ports.add(newPort))
					{
							return SUCCESS;            
					}
					else
					{
							Error = "There was some error trying to remove the port";
							atError();
							return FAILURE;
					}
			}
			else
			{
					Error="Maximum number of ports for the task is already assigned.";
					atError();
			}
			return FAILURE;
	}

	/**
		Remove the port from the list of ports owned by this task.
		@param oldPort
		@return 
		@OSPProject Tasks 
	*/ 
	public int do_removePort(PortCB oldPort)
	{
		// your code goes here
		if (this.Ports.contains(oldPort))
		{
			if (this.Ports.remove(oldPort))
			{
				return SUCCESS;
			}
			else
			{
				Error = "There was some error removing the port";
				atError();
				return FAILURE;
			}
		}
		else
		{
			atWarning();
			Warning = "Attempt to remove unknown port for a task";
		}
		return FAILURE;
	}

	/**
		Insert file into the open files table of the task.
		This methods could also use a SUCCESS or FAILURE return type
		@param file
		@OSPProject Tasks
	*/
	public void do_addFile(OpenFile file)
	{
		// your code goes here
		if (Files.add(file))
		{
			//Successfully added the file
		}
		else
		{
			Error = "There was some error adding file";
			atError();
		}
	}

	/** 
		Remove file from the task's open files table.
		@param file
		@return SUCCESS or FAILURE 
		@OSPProject Tasks
	*/
	public int do_removeFile(OpenFile file)
	{
		if (this.Files.contains(file))
		{
			if (this.Files.remove(file))
			{
				return SUCCESS;
			}
			else
			{
				Error = "There was some error removing the file from the tasks.";
				atError();
				return FAILURE;
			}
		}
		else
		{
			Warning = "Attempting to remove file that does not exist!";
			atWarning();
		}
		return FAILURE;
	}

	/**
		Called by OSP after printing an error message. The student can
		insert code here to print various tables and data structures
		in their state just after the error happened.  The body can be
		left empty, if this feature is not used.
		
		@OSPProject Tasks
	*/
	public static void atError()
	{
		System.out.println("There was an error: "+Error);
	}

	/**
		Called by OSP after printing a warning message. The student
		can insert code here to print various tables and data
		structures in their state just after the warning happened.
		The body can be left empty, if this feature is not used.
		
		@OSPProject Tasks
	*/
	public static void atWarning()
	{
		System.out.println("There was an error: "+Warning);
	}
}