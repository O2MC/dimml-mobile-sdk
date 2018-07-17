//
//  O2MC.m
//  O2MC
//
//  Created by Nicky Romeijn on 09-08-16.
//  Copyright © 2016 Adversitement. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "O2MC.h"
#import "O2MTagger.h"

@implementation O2MC
 -(id)init :(NSString *)endpoint :(NSNumber *)dispatchInterval; {
     self->_tracker =  [[O2MTagger alloc] init:endpoint :dispatchInterval];
    return self;
}
@end

