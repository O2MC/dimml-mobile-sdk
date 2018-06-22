//
//  Datastreams.h
//  Datastreams
//
//  Created by Nicky Romeijn on 16-06-16.
//  Copyright © 2016 Adversitement. All rights reserved.
//

#import <Foundation/Foundation.h>

@class Tagger;

@interface O2MC : NSObject {
}

@property (readonly, nonatomic) Tagger *tracker;

-(id)init :(NSString *)appId :(NSString *)endpoint :(NSNumber *)dispatchInterval :(Boolean) forceStartTimer;

@end
