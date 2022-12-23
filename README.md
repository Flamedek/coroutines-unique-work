Launch tagged tasks in a CoroutineScope and automatically cancel currently running tasks with the same tag. 

Introduces a Task element which can be added to any CoroutineContext to mark it's execution as unique.  
If the same unique tag is detected in a different context, the running jobs will be cancelled.