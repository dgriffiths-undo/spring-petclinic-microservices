/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.customers.web;

import io.micrometer.core.annotation.Timed;
import io.undo.lr.UndoLR;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Optional;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Maciej Szarlinski
 */
@RequestMapping("/owners")
@RestController
@Timed("petclinic.owner")
@RequiredArgsConstructor
@Slf4j
class OwnerResource {

    private final OwnerRepository ownerRepository;

    /**
     * Create Owner
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Owner createOwner(@Valid @RequestBody Owner owner) {
        return ownerRepository.save(owner);
    }

    /**
     * Read single Owner
     */
    @GetMapping(value = "/{ownerId}")
    public Optional<Owner> findOwner(@PathVariable("ownerId") @Min(1) int ownerId) {
        log.info("Finding owner {}", ownerId);
        Optional<Owner> owner = ownerRepository.findById(ownerId);
        if(owner.isPresent()) {
            log.info("Found owner {}", owner.get());
            if ("Peter".equalsIgnoreCase(owner.get().getFirstName())) {
                throw new RuntimeException("simulate RuntimeException bug");
            } else if ("Maria".equalsIgnoreCase(owner.get().getFirstName())) {
                log.info("simulate not finding owner");
                return Optional.empty();
            }
        }
        return owner;
    }

    /**
     * Read List of Owners
     */
    @GetMapping
    public List<Owner> findAll() {
        return ownerRepository.findAll();
    }

    /**
     * Update Owner
     */
    @PutMapping(value = "/{ownerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOwner(@PathVariable("ownerId") @Min(1) int ownerId, @Valid @RequestBody Owner ownerRequest) {
        final Optional<Owner> owner = ownerRepository.findById(ownerId);
        final Owner ownerModel = owner.orElseThrow(() -> new ResourceNotFoundException("Owner "+ownerId+" not found"));

        // This is done by hand for simplicity purpose. In a real life use-case we should consider using MapStruct.
        ownerModel.setFirstName(ownerRequest.getFirstName());
        ownerModel.setLastName(ownerRequest.getLastName());
        ownerModel.setCity(ownerRequest.getCity());
        ownerModel.setAddress(ownerRequest.getAddress());
        ownerModel.setTelephone(ownerRequest.getTelephone());
        log.info("Saving owner {}", ownerModel);
        ownerRepository.save(ownerModel);
    }

    @GetMapping(value = "/startRecording")
    @ResponseStatus(HttpStatus.OK)
    public void startRecording() {
        log.info("start recording");
        try {
            UndoLR.start();
        } catch (Exception e) {
            log.error("UndoLR.start failed", e);
        }
    }

    @GetMapping(value = "/saveRecording/**")
    public String saveRecording(HttpServletRequest request) {
        String filename =
                new AntPathMatcher()
                        .extractPathWithinPattern(
                                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
                                        .toString(),
                                request.getRequestURI());
        log.info("save recording to {}", filename);
        try {
            UndoLR.save(filename);
            log.info("recording saved");
            UndoLR.stop();
            log.info("recording stopped");
        } catch (Exception e) {
            log.error("UndoLR.save failed", e);
        }
        return "Recording saved to " + filename + "\n";
    }
}
