Introduces a UniqueWork tag which can be added to a CoroutineContext to mark it as Unique.  
If the same unique tag is detected in a different context, the initial job will be cancelled.