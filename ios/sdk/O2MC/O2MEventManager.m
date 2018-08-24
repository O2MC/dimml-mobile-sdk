//
//  O2MEventManager.m
//  datastreams
//
//  Created by Tim Slot on 18/07/2018.
//  Copyright © 2018 Adversitement. All rights reserved.
//

#import "O2MEventManager.h"

@interface O2MEventManager()

@property (nonatomic, readonly, strong) dispatch_queue_t eventQueue;

@end

@implementation O2MEventManager

-(instancetype) initWithTagger:(O2MTagger*)tagger; {
    if (self = [super init]) {
        _events = [[NSMutableArray alloc] init];
        _eventQueue = dispatch_queue_create("io.o2mc.sdk", DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

-(void) addEvent :(O2MEvent*)event; {
    dispatch_async(self->_eventQueue,^{
        [self->_events addObject:event];
    });
}

-(void) clearEvents; {
    dispatch_async(self->_eventQueue, ^{
        [self->_events removeAllObjects];
    });
}

-(long) eventCount; {
    __block long count;

    dispatch_sync(self->_eventQueue, ^{
        count = self->_events.count;
    });

    return count;
}

@end
