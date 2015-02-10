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

/**
    The student module dealing with the creation and killing of
    tasks.  A task acts primarily as a container for threads and as
    a holder of resources.  Execution is associated entirely with
    threads.  The primary methods that the student will implement
    are do_create() and do_kill().

    @OSPProject Tasks
*/
public class TaskCB extends IflTaskCB
{
    private List<OpenFile> Files;
    private List<ThreadCB> Threads;
    private List<PortCB> Ports;
    
    private PageTable PT;
    
    static String Warning;
    static String Error;
    
    private ThreadCB TCB;
    private OpenFile SwapPathFile;

    private String SwapPath;
    private FileSys SwapFile;		//swap file

    /**
       The task constructor.

       @OSPProject Tasks
    */
    public TaskCB()
    {
        super();
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Tasks
    */
    public static void init()
    {

    }

    /**
        Creates a new task and sets its properties.

        @return task or null

        @OSPProject Tasks
    */
    static public TaskCB do_create()
    {
        TaskCB NewTask = new TaskCB();
        NewTask.Threads = new ArrayList<>();
        NewTask.Ports = new ArrayList<>();
        NewTask.Files = new ArrayList<>();
       
        PageTable PT = new PageTable(NewTask);
        NewTask.setPageTable(PT);
        NewTask.PT = PT;

        NewTask.setCreationTime(HClock.get());
        NewTask.setStatus(TaskLive);
        NewTask.setPriority(0);
        String SwapPath = SwapDeviceMountPoint + Integer.toString(NewTask.getID());

        
        NewTask.SwapFile.create(SwapPath, (int)Math.pow(2.0D, MMU.getVirtualAddressBits()));
        OpenFile SwapPathFile = OpenFile.open(SwapPath, NewTask);

        NewTask.setSwapFile(SwapPathFile);

        ThreadCB.create(NewTask);        
        
       
        if (SwapPathFile == null)
        {
            //**** add test for SwapFileSuccess == FAILURE - if so, according to p.48, dispatch a new thread, and return null
            // shouldn't be necessary to specifically call dispatch() for the thread, according to bottom of p.48 - footnote 1
            Error = "There was some error in Swap file allocation";
            NewTask.Threads.get(0).dispatch();	//caso não tenha, dispachamos a thread criada acima (posição 0 do vetor)
            return null;
        }
        return NewTask;
    }

    /**
       Kills the specified task and all of its threads.
       
       @OSPProject Tasks
    */
    public void do_kill()
    {
        int Size = this.Threads.size();
        while (Size > 0) 
        {
            this.Threads.get(Size-1).kill();
            Size--;			
        }

        Size = this.Ports.size();
        while (Size > 0) 
        {
            this.Ports.get(Size-1).destroy();
            Size--;			
        }

        Size = this.Files.size();
        while (Size > 0) 
        {
            this.Files.get(Size-1).close();
            Size--;
        }
        
        this.SwapFile.delete(SwapDeviceMountPoint + String.valueOf(this.getID()));

        this.setStatus(TaskTerm);

        this.getPageTable().deallocateMemory();
        this.setStatus(TaskTerm);
        this.PT.deallocateMemory();
       
    }

    public int do_getThreadCount()
    {
        // your code goes here
        return this.Threads.size();
    }

    /**
       Adds the specified thread to this task. 
     * @param thread
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
     * @param thread
     * @return 
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
     * @return
       @OSPProject Tasks
    */
    public int do_getPortCount()
    {
        // your code goes here
        return this.Ports.size();
    }

    /**
       Add the port to the list of ports owned by this task.
     * @param newPort	
     * @return 	
       @OSPProject Tasks 
    */ 
    public int do_addPort(PortCB newPort)
    {
        // your code goes here
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
     * @param oldPort
     * @return 
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
     * @param file
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
     * @param file
     * @return 
	@OSPProject Tasks
    */
    public int do_removeFile(OpenFile file)
    {
        // your code goes here
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
        // your code goes here
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
        // your code goes here
        System.out.println("There was an error: "+Warning);
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */
}