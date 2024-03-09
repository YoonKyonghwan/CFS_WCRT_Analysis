from dataclasses import dataclass, field
from decimal import Decimal
from typing import List, Optional


@dataclass
class ConstraintsModel:
    class Meta:
        name = "constraintsModel"

    event_chains: List["ConstraintsModel.EventChains"] = field(
        default_factory=list,
        metadata={
            "name": "eventChains",
            "type": "Element",
            "min_occurs": 1,
        }
    )
    requirements: List["ConstraintsModel.Requirements"] = field(
        default_factory=list,
        metadata={
            "type": "Element",
            "min_occurs": 1,
        }
    )

    @dataclass
    class EventChains:
        segments: List["ConstraintsModel.EventChains.Segments"] = field(
            default_factory=list,
            metadata={
                "type": "Element",
                "min_occurs": 1,
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        stimulus: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        response: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class Segments:
            event_chain: Optional["ConstraintsModel.EventChains.Segments.EventChain"] = field(
                default=None,
                metadata={
                    "name": "eventChain",
                    "type": "Element",
                    "required": True,
                }
            )

            @dataclass
            class EventChain:
                name: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )
                stimulus: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )
                response: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )

    @dataclass
    class Requirements:
        limit: Optional["ConstraintsModel.Requirements.Limit"] = field(
            default=None,
            metadata={
                "type": "Element",
                "required": True,
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        process: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class Limit:
            limit_value: Optional["ConstraintsModel.Requirements.Limit.LimitValue"] = field(
                default=None,
                metadata={
                    "name": "limitValue",
                    "type": "Element",
                    "required": True,
                }
            )
            limit_type: Optional[str] = field(
                default=None,
                metadata={
                    "name": "limitType",
                    "type": "Attribute",
                    "required": True,
                }
            )
            metric: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )

            @dataclass
            class LimitValue:
                value: Optional[int] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )
                unit: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )


@dataclass
class EventModel:
    class Meta:
        name = "eventModel"

    events: List["EventModel.Events"] = field(
        default_factory=list,
        metadata={
            "type": "Element",
            "min_occurs": 1,
        }
    )

    @dataclass
    class Events:
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        event_type: Optional[str] = field(
            default=None,
            metadata={
                "name": "eventType",
                "type": "Attribute",
                "required": True,
            }
        )
        entity: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        description: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
            }
        )


@dataclass
class HwModel:
    class Meta:
        name = "hwModel"

    core_types: Optional["HwModel.CoreTypes"] = field(
        default=None,
        metadata={
            "name": "coreTypes",
            "type": "Element",
            "required": True,
        }
    )
    memory_types: List["HwModel.MemoryTypes"] = field(
        default_factory=list,
        metadata={
            "name": "memoryTypes",
            "type": "Element",
            "min_occurs": 1,
        }
    )
    network_types: List["HwModel.NetworkTypes"] = field(
        default_factory=list,
        metadata={
            "name": "networkTypes",
            "type": "Element",
            "min_occurs": 1,
        }
    )
    access_paths: List["HwModel.AccessPaths"] = field(
        default_factory=list,
        metadata={
            "name": "accessPaths",
            "type": "Element",
            "min_occurs": 1,
        }
    )
    system: Optional["HwModel.System"] = field(
        default=None,
        metadata={
            "type": "Element",
            "required": True,
        }
    )

    @dataclass
    class CoreTypes:
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        bit_width: Optional[int] = field(
            default=None,
            metadata={
                "name": "bitWidth",
                "type": "Attribute",
                "required": True,
            }
        )
        instructions_per_cycle: Optional[int] = field(
            default=None,
            metadata={
                "name": "instructionsPerCycle",
                "type": "Attribute",
                "required": True,
            }
        )

    @dataclass
    class MemoryTypes:
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        x_access_pattern: Optional[str] = field(
            default=None,
            metadata={
                "name": "xAccessPattern",
                "type": "Attribute",
                "required": True,
            }
        )
        type: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        size: Optional[int] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

    @dataclass
    class NetworkTypes:
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        bit_width: Optional[int] = field(
            default=None,
            metadata={
                "name": "bitWidth",
                "type": "Attribute",
            }
        )
        bus_type: Optional[str] = field(
            default=None,
            metadata={
                "name": "busType",
                "type": "Attribute",
            }
        )

    @dataclass
    class AccessPaths:
        latencies: Optional["HwModel.AccessPaths.Latencies"] = field(
            default=None,
            metadata={
                "type": "Element",
                "required": True,
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        source: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        target: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class Latencies:
            access_type: Optional[str] = field(
                default=None,
                metadata={
                    "name": "accessType",
                    "type": "Attribute",
                    "required": True,
                }
            )
            transfer_size: Optional[int] = field(
                default=None,
                metadata={
                    "name": "transferSize",
                    "type": "Attribute",
                    "required": True,
                }
            )
            quartz: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )
            value: Optional[int] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )

    @dataclass
    class System:
        ecus: Optional["HwModel.System.Ecus"] = field(
            default=None,
            metadata={
                "type": "Element",
                "required": True,
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class Ecus:
            microcontrollers: Optional["HwModel.System.Ecus.Microcontrollers"] = field(
                default=None,
                metadata={
                    "type": "Element",
                    "required": True,
                }
            )
            name: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )

            @dataclass
            class Microcontrollers:
                memories: List["HwModel.System.Ecus.Microcontrollers.Memories"] = field(
                    default_factory=list,
                    metadata={
                        "type": "Element",
                        "min_occurs": 1,
                    }
                )
                networks: Optional["HwModel.System.Ecus.Microcontrollers.Networks"] = field(
                    default=None,
                    metadata={
                        "type": "Element",
                        "required": True,
                    }
                )
                quartzes: Optional["HwModel.System.Ecus.Microcontrollers.Quartzes"] = field(
                    default=None,
                    metadata={
                        "type": "Element",
                        "required": True,
                    }
                )
                cores: List["HwModel.System.Ecus.Microcontrollers.Cores"] = field(
                    default_factory=list,
                    metadata={
                        "type": "Element",
                        "min_occurs": 1,
                    }
                )
                name: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )

                @dataclass
                class Memories:
                    ports: List["HwModel.System.Ecus.Microcontrollers.Memories.Ports"] = field(
                        default_factory=list,
                        metadata={
                            "type": "Element",
                            "min_occurs": 1,
                        }
                    )
                    name: Optional[str] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )
                    type: Optional[str] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )

                    @dataclass
                    class Ports:
                        name: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        network: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        bit_width: Optional[int] = field(
                            default=None,
                            metadata={
                                "name": "bitWidth",
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        direction: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        write_cycles: Optional[int] = field(
                            default=None,
                            metadata={
                                "name": "writeCycles",
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        read_cycles: Optional[int] = field(
                            default=None,
                            metadata={
                                "name": "readCycles",
                                "type": "Attribute",
                                "required": True,
                            }
                        )

                @dataclass
                class Networks:
                    prescaler: Optional["HwModel.System.Ecus.Microcontrollers.Networks.Prescaler"] = field(
                        default=None,
                        metadata={
                            "type": "Element",
                            "required": True,
                        }
                    )
                    name: Optional[str] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )
                    type: Optional[str] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )

                    @dataclass
                    class Prescaler:
                        name: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        clock_ratio: Optional[Decimal] = field(
                            default=None,
                            metadata={
                                "name": "clockRatio",
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        quartz: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )

                @dataclass
                class Quartzes:
                    name: Optional[str] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )
                    frequency: Optional[int] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )

                @dataclass
                class Cores:
                    networks: Optional["HwModel.System.Ecus.Microcontrollers.Cores.Networks"] = field(
                        default=None,
                        metadata={
                            "type": "Element",
                            "required": True,
                        }
                    )
                    ports: List["HwModel.System.Ecus.Microcontrollers.Cores.Ports"] = field(
                        default_factory=list,
                        metadata={
                            "type": "Element",
                            "min_occurs": 1,
                        }
                    )
                    prescaler: Optional["HwModel.System.Ecus.Microcontrollers.Cores.Prescaler"] = field(
                        default=None,
                        metadata={
                            "type": "Element",
                            "required": True,
                        }
                    )
                    name: Optional[str] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )
                    core_type: Optional[str] = field(
                        default=None,
                        metadata={
                            "name": "coreType",
                            "type": "Attribute",
                            "required": True,
                        }
                    )

                    @dataclass
                    class Networks:
                        name: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        type: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )

                    @dataclass
                    class Ports:
                        name: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        network: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        is_master: Optional[bool] = field(
                            default=None,
                            metadata={
                                "name": "isMaster",
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        bit_width: Optional[int] = field(
                            default=None,
                            metadata={
                                "name": "bitWidth",
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        direction: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        write_cycles: Optional[int] = field(
                            default=None,
                            metadata={
                                "name": "writeCycles",
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        read_cycles: Optional[int] = field(
                            default=None,
                            metadata={
                                "name": "readCycles",
                                "type": "Attribute",
                                "required": True,
                            }
                        )

                    @dataclass
                    class Prescaler:
                        name: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        clock_ratio: Optional[Decimal] = field(
                            default=None,
                            metadata={
                                "name": "clockRatio",
                                "type": "Attribute",
                                "required": True,
                            }
                        )
                        quartz: Optional[str] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )


@dataclass
class MappingModel:
    class Meta:
        name = "mappingModel"

    process_allocation: List["MappingModel.ProcessAllocation"] = field(
        default_factory=list,
        metadata={
            "name": "processAllocation",
            "type": "Element",
            "min_occurs": 1,
        }
    )
    core_allocation: List["MappingModel.CoreAllocation"] = field(
        default_factory=list,
        metadata={
            "name": "coreAllocation",
            "type": "Element",
            "min_occurs": 1,
        }
    )
    mapping: List["MappingModel.Mapping"] = field(
        default_factory=list,
        metadata={
            "type": "Element",
            "min_occurs": 1,
        }
    )
    address_mapping_type: Optional[str] = field(
        default=None,
        metadata={
            "name": "addressMappingType",
            "type": "Attribute",
            "required": True,
        }
    )

    @dataclass
    class ProcessAllocation:
        process: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        scheduler: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

    @dataclass
    class CoreAllocation:
        scheduler: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        core: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

    @dataclass
    class Mapping:
        mem: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        abstract_element: Optional[str] = field(
            default=None,
            metadata={
                "name": "abstractElement",
                "type": "Attribute",
                "required": True,
            }
        )


@dataclass
class OsModel:
    class Meta:
        name = "osModel"

    operating_systems: Optional["OsModel.OperatingSystems"] = field(
        default=None,
        metadata={
            "name": "operatingSystems",
            "type": "Element",
            "required": True,
        }
    )

    @dataclass
    class OperatingSystems:
        task_schedulers: List["OsModel.OperatingSystems.TaskSchedulers"] = field(
            default_factory=list,
            metadata={
                "name": "taskSchedulers",
                "type": "Element",
                "min_occurs": 1,
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class TaskSchedulers:
            scheduling_unit: Optional["OsModel.OperatingSystems.TaskSchedulers.SchedulingUnit"] = field(
                default=None,
                metadata={
                    "name": "schedulingUnit",
                    "type": "Element",
                    "required": True,
                }
            )
            scheduling_algorithm: Optional[object] = field(
                default=None,
                metadata={
                    "name": "schedulingAlgorithm",
                    "type": "Element",
                }
            )
            name: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )

            @dataclass
            class SchedulingUnit:
                delay: Optional["OsModel.OperatingSystems.TaskSchedulers.SchedulingUnit.Delay"] = field(
                    default=None,
                    metadata={
                        "type": "Element",
                        "required": True,
                    }
                )

                @dataclass
                class Delay:
                    unit: Optional[str] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )


@dataclass
class StimuliModel:
    class Meta:
        name = "stimuliModel"

    stimuli: List["StimuliModel.Stimuli"] = field(
        default_factory=list,
        metadata={
            "type": "Element",
            "min_occurs": 1,
        }
    )

    @dataclass
    class Stimuli:
        stimulus_deviation: Optional["StimuliModel.Stimuli.StimulusDeviation"] = field(
            default=None,
            metadata={
                "name": "stimulusDeviation",
                "type": "Element",
            }
        )
        offset: Optional["StimuliModel.Stimuli.Offset"] = field(
            default=None,
            metadata={
                "type": "Element",
            }
        )
        recurrence: Optional["StimuliModel.Stimuli.Recurrence"] = field(
            default=None,
            metadata={
                "type": "Element",
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class StimulusDeviation:
            lower_bound: Optional["StimuliModel.Stimuli.StimulusDeviation.LowerBound"] = field(
                default=None,
                metadata={
                    "name": "lowerBound",
                    "type": "Element",
                    "required": True,
                }
            )
            upper_bound: Optional["StimuliModel.Stimuli.StimulusDeviation.UpperBound"] = field(
                default=None,
                metadata={
                    "name": "upperBound",
                    "type": "Element",
                    "required": True,
                }
            )
            distribution: Optional[object] = field(
                default=None,
                metadata={
                    "type": "Element",
                }
            )

            @dataclass
            class LowerBound:
                value: Optional[int] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )
                unit: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )

            @dataclass
            class UpperBound:
                value: Optional[int] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )
                unit: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )

        @dataclass
        class Offset:
            unit: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )

        @dataclass
        class Recurrence:
            value: Optional[int] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )
            unit: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )


@dataclass
class SwModel:
    class Meta:
        name = "swModel"

    tasks: List["SwModel.Tasks"] = field(
        default_factory=list,
        metadata={
            "type": "Element",
            "min_occurs": 1,
        }
    )
    runnables: List["SwModel.Runnables"] = field(
        default_factory=list,
        metadata={
            "type": "Element",
            "min_occurs": 1,
        }
    )
    labels: List["SwModel.Labels"] = field(
        default_factory=list,
        metadata={
            "type": "Element",
            "min_occurs": 1,
        }
    )
    activations: List["SwModel.Activations"] = field(
        default_factory=list,
        metadata={
            "type": "Element",
            "min_occurs": 1,
        }
    )

    @dataclass
    class Tasks:
        call_graph: Optional["SwModel.Tasks.CallGraph"] = field(
            default=None,
            metadata={
                "name": "callGraph",
                "type": "Element",
                "required": True,
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        priority: Optional[int] = field(
            default=None,
            metadata={
                "type": "Attribute",
            }
        )
        stimuli: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        preemption: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        multiple_task_activation_limit: Optional[int] = field(
            default=None,
            metadata={
                "name": "multipleTaskActivationLimit",
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class CallGraph:
            graph_entries: Optional["SwModel.Tasks.CallGraph.GraphEntries"] = field(
                default=None,
                metadata={
                    "name": "graphEntries",
                    "type": "Element",
                    "required": True,
                }
            )

            @dataclass
            class GraphEntries:
                calls: List["SwModel.Tasks.CallGraph.GraphEntries.Calls"] = field(
                    default_factory=list,
                    metadata={
                        "type": "Element",
                        "min_occurs": 1,
                    }
                )
                name: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )

                @dataclass
                class Calls:
                    runnable: Optional[str] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )

    @dataclass
    class Runnables:
        runnable_items: List["SwModel.Runnables.RunnableItems"] = field(
            default_factory=list,
            metadata={
                "name": "runnableItems",
                "type": "Element",
                "min_occurs": 1,
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        activation: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class RunnableItems:
            deviation: Optional["SwModel.Runnables.RunnableItems.Deviation"] = field(
                default=None,
                metadata={
                    "type": "Element",
                }
            )
            data: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                }
            )
            access: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                }
            )

            @dataclass
            class Deviation:
                lower_bound: Optional["SwModel.Runnables.RunnableItems.Deviation.LowerBound"] = field(
                    default=None,
                    metadata={
                        "name": "lowerBound",
                        "type": "Element",
                        "required": True,
                    }
                )
                upper_bound: Optional["SwModel.Runnables.RunnableItems.Deviation.UpperBound"] = field(
                    default=None,
                    metadata={
                        "name": "upperBound",
                        "type": "Element",
                        "required": True,
                    }
                )
                distribution: Optional["SwModel.Runnables.RunnableItems.Deviation.Distribution"] = field(
                    default=None,
                    metadata={
                        "type": "Element",
                        "required": True,
                    }
                )

                @dataclass
                class LowerBound:
                    value: Optional[int] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )

                @dataclass
                class UpperBound:
                    value: Optional[int] = field(
                        default=None,
                        metadata={
                            "type": "Attribute",
                            "required": True,
                        }
                    )

                @dataclass
                class Distribution:
                    mean: Optional["SwModel.Runnables.RunnableItems.Deviation.Distribution.Mean"] = field(
                        default=None,
                        metadata={
                            "type": "Element",
                            "required": True,
                        }
                    )
                    p_remain_promille: Optional[float] = field(
                        default=None,
                        metadata={
                            "name": "pRemainPromille",
                            "type": "Attribute",
                            "required": True,
                        }
                    )

                    @dataclass
                    class Mean:
                        value: Optional[int] = field(
                            default=None,
                            metadata={
                                "type": "Attribute",
                                "required": True,
                            }
                        )

    @dataclass
    class Labels:
        size: Optional["SwModel.Labels.Size"] = field(
            default=None,
            metadata={
                "type": "Element",
                "required": True,
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )
        constant: Optional[bool] = field(
            default=None,
            metadata={
                "type": "Attribute",
            }
        )

        @dataclass
        class Size:
            number_bits: Optional[int] = field(
                default=None,
                metadata={
                    "name": "numberBits",
                    "type": "Attribute",
                    "required": True,
                }
            )

    @dataclass
    class Activations:
        custom_properties: List["SwModel.Activations.CustomProperties"] = field(
            default_factory=list,
            metadata={
                "name": "customProperties",
                "type": "Element",
            }
        )
        min: Optional["SwModel.Activations.Min"] = field(
            default=None,
            metadata={
                "type": "Element",
            }
        )
        max: Optional["SwModel.Activations.Max"] = field(
            default=None,
            metadata={
                "type": "Element",
            }
        )
        name: Optional[str] = field(
            default=None,
            metadata={
                "type": "Attribute",
                "required": True,
            }
        )

        @dataclass
        class CustomProperties:
            value: Optional["SwModel.Activations.CustomProperties.Value"] = field(
                default=None,
                metadata={
                    "type": "Element",
                    "required": True,
                }
            )
            key: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )

            @dataclass
            class Value:
                value: Optional[str] = field(
                    default=None,
                    metadata={
                        "type": "Attribute",
                        "required": True,
                    }
                )

        @dataclass
        class Min:
            value: Optional[int] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )
            unit: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )

        @dataclass
        class Max:
            value: Optional[int] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )
            unit: Optional[str] = field(
                default=None,
                metadata={
                    "type": "Attribute",
                    "required": True,
                }
            )
